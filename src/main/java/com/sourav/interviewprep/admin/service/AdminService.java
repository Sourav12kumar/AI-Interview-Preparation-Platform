package com.sourav.interviewprep.admin.service;

import com.sourav.interviewprep.admin.audit.AuditEventEntity;
import com.sourav.interviewprep.admin.audit.AuditEventRepository;
import com.sourav.interviewprep.admin.audit.AuditService;
import com.sourav.interviewprep.admin.dto.AdminCodingProblemRequest;
import com.sourav.interviewprep.admin.dto.AdminCodingProblemResponse;
import com.sourav.interviewprep.admin.dto.AdminOverviewResponse;
import com.sourav.interviewprep.admin.dto.AdminUserResponse;
import com.sourav.interviewprep.admin.dto.AdminUserStatusRequest;
import com.sourav.interviewprep.admin.dto.AuditEventResponse;
import com.sourav.interviewprep.admin.exception.AdminOperationException;
import com.sourav.interviewprep.admin.exception.DuplicateCodingProblemException;
import com.sourav.interviewprep.auth.entity.AccountStatus;
import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.coding.entity.CodingProblemEntity;
import com.sourav.interviewprep.coding.entity.ProgrammingLanguage;
import com.sourav.interviewprep.coding.exception.CodingProblemNotFoundException;
import com.sourav.interviewprep.coding.repository.CodingProblemRepository;
import com.sourav.interviewprep.coding.repository.CodingSubmissionRepository;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminService {

    private static final int MAX_LIST_LIMIT = 200;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CodingProblemRepository problemRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final AuditEventRepository auditEventRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AdminService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            CodingProblemRepository problemRepository,
            CodingSubmissionRepository submissionRepository,
            InterviewSessionRepository sessionRepository,
            AuditEventRepository auditEventRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.sessionRepository = sessionRepository;
        this.auditEventRepository = auditEventRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse overview() {
        return new AdminOverviewResponse(
                userRepository.count(),
                userRepository.countByAccountStatus(AccountStatus.ACTIVE),
                userRepository.countByAccountStatus(AccountStatus.SUSPENDED),
                problemRepository.count(),
                problemRepository.countByActiveTrue(),
                submissionRepository.count(),
                sessionRepository.count());
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(String query, AccountStatus status, int limit) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(user -> status == null || user.getAccountStatus() == status)
                .filter(user -> normalized.isBlank()
                        || user.getEmail().toLowerCase(Locale.ROOT).contains(normalized)
                        || user.getFullName().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(normalizeLimit(limit))
                .map(this::userResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse changeUserStatus(
            String actorEmail,
            Long userId,
            AdminUserStatusRequest request) {
        UserEntity actor = currentActor(actorEmail);
        UserEntity target = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (actor.getId().equals(target.getId())) {
            throw new AdminOperationException("Administrators cannot change their own account status");
        }
        if (request.status() != AccountStatus.ACTIVE && request.status() != AccountStatus.SUSPENDED) {
            throw new AdminOperationException("Administrators may set only ACTIVE or SUSPENDED status");
        }
        AccountStatus previous = target.getAccountStatus();
        target.changeStatus(request.status());
        userRepository.saveAndFlush(target);
        if (request.status() == AccountStatus.SUSPENDED) {
            refreshTokenRepository.revokeAllActiveByUserId(target.getId(), Instant.now());
        }
        auditService.success(actor, "USER_STATUS_CHANGED", "USER", target.getId().toString(), Map.of(
                "previousStatus", previous.name(),
                "newStatus", request.status().name()));
        return userResponse(target);
    }

    @Transactional(readOnly = true)
    public List<AdminCodingProblemResponse> listProblems() {
        return problemRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::problemResponse)
                .toList();
    }

    @Transactional
    public AdminCodingProblemResponse createProblem(
            String actorEmail,
            AdminCodingProblemRequest request) {
        UserEntity actor = currentActor(actorEmail);
        validateProblem(request);
        if (problemRepository.existsBySlugIgnoreCase(request.slug())) {
            throw new DuplicateCodingProblemException();
        }
        CodingProblemEntity problem = problemRepository.saveAndFlush(new CodingProblemEntity(
                request.title().trim(), request.slug(), request.description().trim(), request.difficulty(),
                writeJson(request.starterCode()), writeJson(request.testCases()), writeJson(request.tags()),
                request.active()));
        auditService.success(actor, "CODING_PROBLEM_CREATED", "CODING_PROBLEM", problem.getId().toString(),
                Map.of("slug", problem.getSlug()));
        return problemResponse(problem);
    }

    @Transactional
    public AdminCodingProblemResponse updateProblem(
            String actorEmail,
            Long problemId,
            AdminCodingProblemRequest request) {
        UserEntity actor = currentActor(actorEmail);
        validateProblem(request);
        CodingProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(CodingProblemNotFoundException::new);
        if (problemRepository.existsBySlugIgnoreCaseAndIdNot(request.slug(), problemId)) {
            throw new DuplicateCodingProblemException();
        }
        problem.update(
                request.title().trim(), request.slug(), request.description().trim(), request.difficulty(),
                writeJson(request.starterCode()), writeJson(request.testCases()), writeJson(request.tags()),
                request.active());
        problemRepository.saveAndFlush(problem);
        auditService.success(actor, "CODING_PROBLEM_UPDATED", "CODING_PROBLEM", problem.getId().toString(),
                Map.of("slug", problem.getSlug(), "active", problem.isActive()));
        return problemResponse(problem);
    }

    @Transactional
    public void deactivateProblem(String actorEmail, Long problemId) {
        UserEntity actor = currentActor(actorEmail);
        CodingProblemEntity problem = problemRepository.findById(problemId)
                .orElseThrow(CodingProblemNotFoundException::new);
        problem.deactivate();
        problemRepository.saveAndFlush(problem);
        auditService.success(actor, "CODING_PROBLEM_DEACTIVATED", "CODING_PROBLEM", problem.getId().toString(),
                Map.of("slug", problem.getSlug()));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listAuditEvents(int limit) {
        return auditEventRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(0, normalizeLimit(limit))).stream()
                .map(this::auditResponse)
                .toList();
    }

    private void validateProblem(AdminCodingProblemRequest request) {
        if (!request.starterCode().isObject() || request.starterCode().isEmpty()) {
            throw new AdminOperationException("Starter code must be a non-empty JSON object");
        }
        boolean supportedStarter = false;
        for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
            JsonNode node = request.starterCode().path(language.name());
            if (node.isTextual() && !node.asText().isBlank()) supportedStarter = true;
        }
        if (!supportedStarter) {
            throw new AdminOperationException("Starter code must include a supported language");
        }
        if (!request.testCases().isArray() || request.testCases().isEmpty()
                || request.testCases().size() > 100) {
            throw new AdminOperationException("Test cases must contain between 1 and 100 items");
        }
        request.testCases().forEach(node -> {
            if (!node.isObject() || !node.has("input") || !node.has("expected")) {
                throw new AdminOperationException(
                        "Each test case must contain input and expected values");
            }
        });
        if (!request.tags().isArray() || request.tags().isEmpty() || request.tags().size() > 20) {
            throw new AdminOperationException("Tags must contain between 1 and 20 items");
        }
        request.tags().forEach(node -> {
            if (!node.isTextual() || node.asText().isBlank() || node.asText().length() > 60) {
                throw new AdminOperationException("Each tag must be a non-empty string up to 60 characters");
            }
        });
    }

    private AdminUserResponse userResponse(UserEntity user) {
        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .sorted()
                .toList();
        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getAccountStatus(),
                user.isEmailVerified(), roles, user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private AdminCodingProblemResponse problemResponse(CodingProblemEntity problem) {
        return new AdminCodingProblemResponse(
                problem.getId(), problem.getTitle(), problem.getSlug(), problem.getDescription(),
                problem.getDifficulty(), readJson(problem.getStarterCode()), readJson(problem.getTestCases()),
                readJson(problem.getTags()), problem.isActive(), problem.getCreatedAt(), problem.getUpdatedAt());
    }

    private AuditEventResponse auditResponse(AuditEventEntity event) {
        return new AuditEventResponse(
                event.getId(), event.getActor() == null ? null : event.getActor().getId(),
                event.getAction(), event.getTargetType(), event.getTargetId(), event.getOutcome(),
                event.getCorrelationId(), readJson(event.getMetadata()), event.getCreatedAt());
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json == null ? "null" : json);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored JSON is invalid", exception);
        }
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Coding problem JSON could not be stored", exception);
        }
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) throw new AdminOperationException("Limit must be at least 1");
        return Math.min(limit, MAX_LIST_LIMIT);
    }

    private UserEntity currentActor(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated administrator no longer exists"));
        if (!user.isActive()) {
            throw new UsernameNotFoundException("Authenticated administrator is not active");
        }
        return user;
    }
}
