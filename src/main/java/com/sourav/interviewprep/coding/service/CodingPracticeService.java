package com.sourav.interviewprep.coding.service;

import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.coding.dto.CodingProblemResponse;
import com.sourav.interviewprep.coding.dto.CodingProblemSummaryResponse;
import com.sourav.interviewprep.coding.dto.CodingSubmissionResponse;
import com.sourav.interviewprep.coding.dto.CodingSubmissionSummaryResponse;
import com.sourav.interviewprep.coding.dto.SubmitCodeRequest;
import com.sourav.interviewprep.coding.entity.CodingDifficulty;
import com.sourav.interviewprep.coding.entity.CodingProblemEntity;
import com.sourav.interviewprep.coding.entity.CodingSubmissionEntity;
import com.sourav.interviewprep.coding.exception.CodeExecutionException;
import com.sourav.interviewprep.coding.exception.CodingProblemNotFoundException;
import com.sourav.interviewprep.coding.exception.CodingSubmissionNotFoundException;
import com.sourav.interviewprep.coding.repository.CodingProblemRepository;
import com.sourav.interviewprep.coding.repository.CodingSubmissionRepository;
import com.sourav.interviewprep.coding.runner.CodeExecutionRequest;
import com.sourav.interviewprep.coding.runner.CodeExecutionResult;
import com.sourav.interviewprep.coding.runner.CodeRunner;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CodingPracticeService {

    private static final int TIME_LIMIT_MS = 2_000;
    private static final int MEMORY_LIMIT_MB = 256;
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final UserRepository userRepository;
    private final CodingProblemRepository problemRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final CodeRunner codeRunner;
    private final ObjectMapper objectMapper;

    public CodingPracticeService(
            UserRepository userRepository,
            CodingProblemRepository problemRepository,
            CodingSubmissionRepository submissionRepository,
            CodeRunner codeRunner,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.codeRunner = codeRunner;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CodingProblemSummaryResponse> listProblems(CodingDifficulty difficulty, String tag) {
        String normalizedTag = tag == null ? null : tag.trim().toLowerCase(Locale.ROOT);
        return problemRepository.findAllByActiveTrueOrderByDifficultyAscTitleAsc().stream()
                .filter(problem -> difficulty == null || problem.getDifficulty() == difficulty)
                .filter(problem -> normalizedTag == null || normalizedTag.isBlank()
                        || readTextList(problem.getTags()).stream()
                        .anyMatch(value -> value.toLowerCase(Locale.ROOT).equals(normalizedTag)))
                .map(this::problemSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CodingProblemResponse getProblem(Long problemId) {
        return problemResponse(activeProblem(problemId));
    }

    public CodingSubmissionResponse submit(String email, Long problemId, SubmitCodeRequest request) {
        UserEntity user = currentUser(email);
        CodingProblemEntity problem = activeProblem(problemId);
        JsonNode hiddenTests = readRequiredArray(problem.getTestCases(), "test cases");
        CodeExecutionResult result = codeRunner.execute(new CodeExecutionRequest(
                problem.getSlug(), request.language(), request.sourceCode(), hiddenTests,
                TIME_LIMIT_MS, MEMORY_LIMIT_MB));
        validateResult(result, hiddenTests.size());
        CodingSubmissionEntity submission = submissionRepository.saveAndFlush(
                new CodingSubmissionEntity(user, problem, request.language(), request.sourceCode(), result));
        return submissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<CodingSubmissionSummaryResponse> listSubmissions(String email) {
        UserEntity user = currentUser(email);
        return submissionRepository.findAllByUser_IdOrderBySubmittedAtDesc(user.getId()).stream()
                .map(this::submissionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CodingSubmissionResponse getSubmission(String email, Long submissionId) {
        UserEntity user = currentUser(email);
        CodingSubmissionEntity submission = submissionRepository
                .findByIdAndUser_Id(submissionId, user.getId())
                .orElseThrow(CodingSubmissionNotFoundException::new);
        return submissionResponse(submission);
    }

    private CodingProblemSummaryResponse problemSummary(CodingProblemEntity problem) {
        return new CodingProblemSummaryResponse(
                problem.getId(), problem.getTitle(), problem.getSlug(), problem.getDifficulty(),
                readTextList(problem.getTags()));
    }

    private CodingProblemResponse problemResponse(CodingProblemEntity problem) {
        return new CodingProblemResponse(
                problem.getId(), problem.getTitle(), problem.getSlug(), problem.getDescription(),
                problem.getDifficulty(), readObject(problem.getStarterCode()), readTextList(problem.getTags()));
    }

    private CodingSubmissionSummaryResponse submissionSummary(CodingSubmissionEntity submission) {
        return new CodingSubmissionSummaryResponse(
                submission.getId(), submission.getProblem().getId(), submission.getProblem().getTitle(),
                submission.getLanguage(), submission.getVerdict(), submission.getPassedTestCases(),
                submission.getTotalTestCases(), submission.getScore(), submission.getSubmittedAt());
    }

    private CodingSubmissionResponse submissionResponse(CodingSubmissionEntity submission) {
        return new CodingSubmissionResponse(
                submission.getId(), submission.getProblem().getId(), submission.getProblem().getTitle(),
                submission.getLanguage(), submission.getSourceCode(), submission.getVerdict(),
                submission.getPassedTestCases(), submission.getTotalTestCases(),
                submission.getExecutionTimeMs(), submission.getMemoryUsedKb(), submission.getScore(),
                submission.getResultMessage(), submission.getSubmittedAt());
    }

    private void validateResult(CodeExecutionResult result, int expectedTestCases) {
        if (result == null || result.verdict() == null || !result.verdict().isFinal()) {
            throw new CodeExecutionException("Code runner returned an invalid verdict");
        }
        if (result.totalTestCases() != expectedTestCases || result.passedTestCases() < 0
                || result.passedTestCases() > result.totalTestCases()) {
            throw new CodeExecutionException("Code runner returned invalid test-case counts");
        }
        if (result.executionTimeMs() != null && result.executionTimeMs() < 0) {
            throw new CodeExecutionException("Code runner returned an invalid execution time");
        }
        if (result.memoryUsedKb() != null && result.memoryUsedKb() < 0) {
            throw new CodeExecutionException("Code runner returned invalid memory usage");
        }
        if (result.score() == null || result.score().compareTo(MIN_SCORE) < 0
                || result.score().compareTo(MAX_SCORE) > 0) {
            throw new CodeExecutionException("Code runner returned an invalid score");
        }
    }

    private CodingProblemEntity activeProblem(Long problemId) {
        return problemRepository.findByIdAndActiveTrue(problemId)
                .orElseThrow(CodingProblemNotFoundException::new);
    }

    private JsonNode readObject(String json) {
        try {
            JsonNode node = objectMapper.readTree(json == null ? "{}" : json);
            if (!node.isObject()) throw new IllegalStateException();
            return node;
        } catch (Exception exception) {
            throw new IllegalStateException("Stored starter code is invalid", exception);
        }
    }

    private JsonNode readRequiredArray(String json, String field) {
        try {
            JsonNode node = objectMapper.readTree(json == null ? "[]" : json);
            if (!node.isArray() || node.isEmpty()) throw new IllegalStateException();
            return node;
        } catch (Exception exception) {
            throw new IllegalStateException("Stored " + field + " are invalid", exception);
        }
    }

    private List<String> readTextList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) throw new IllegalStateException();
            List<String> values = new ArrayList<>();
            root.forEach(node -> {
                if (!node.asText("").isBlank()) values.add(node.asText().trim());
            });
            return List.copyOf(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored coding problem tags are invalid", exception);
        }
    }

    private UserEntity currentUser(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user no longer exists"));
        if (!user.isActive()) {
            throw new UsernameNotFoundException("Authenticated user is not active");
        }
        return user;
    }
}
