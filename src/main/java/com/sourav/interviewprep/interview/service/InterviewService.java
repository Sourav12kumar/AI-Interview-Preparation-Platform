package com.sourav.interviewprep.interview.service;

import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.interview.ai.GeneratedQuestion;
import com.sourav.interviewprep.interview.ai.InterviewGenerationContext;
import com.sourav.interviewprep.interview.ai.InterviewQuestionGenerator;
import com.sourav.interviewprep.interview.ai.QuestionGenerationResult;
import com.sourav.interviewprep.interview.dto.CreateInterviewRequest;
import com.sourav.interviewprep.interview.dto.InterviewQuestionResponse;
import com.sourav.interviewprep.interview.dto.InterviewSessionResponse;
import com.sourav.interviewprep.interview.dto.InterviewSessionSummaryResponse;
import com.sourav.interviewprep.interview.entity.InterviewQuestionEntity;
import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import com.sourav.interviewprep.interview.exception.InterviewConfigurationException;
import com.sourav.interviewprep.interview.exception.InterviewNotFoundException;
import com.sourav.interviewprep.interview.repository.InterviewQuestionRepository;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import com.sourav.interviewprep.profile.entity.CandidateProfileEntity;
import com.sourav.interviewprep.profile.entity.TargetCompanyEntity;
import com.sourav.interviewprep.profile.entity.UserSkillEntity;
import com.sourav.interviewprep.profile.repository.CandidateProfileRepository;
import com.sourav.interviewprep.profile.repository.TargetCompanyRepository;
import com.sourav.interviewprep.profile.repository.UserSkillRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final UserSkillRepository userSkillRepository;
    private final TargetCompanyRepository targetCompanyRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewQuestionGenerator questionGenerator;
    private final ObjectMapper objectMapper;

    public InterviewService(
            UserRepository userRepository,
            CandidateProfileRepository profileRepository,
            UserSkillRepository userSkillRepository,
            TargetCompanyRepository targetCompanyRepository,
            InterviewSessionRepository sessionRepository,
            InterviewQuestionRepository questionRepository,
            InterviewQuestionGenerator questionGenerator,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.userSkillRepository = userSkillRepository;
        this.targetCompanyRepository = targetCompanyRepository;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.questionGenerator = questionGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InterviewSessionResponse create(String email, CreateInterviewRequest request) {
        UserEntity user = currentUser(email);
        CandidateProfileEntity profile = profileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new InterviewConfigurationException(
                        "Create a candidate profile before generating an interview"));
        String targetRole = firstNonBlank(request.targetRole(), profile.getTargetRole());
        if (targetRole == null) {
            throw new InterviewConfigurationException(
                    "Set a target role in the request or candidate profile");
        }

        List<UserSkillEntity> userSkills = userSkillRepository
                .findAllByUser_IdOrderBySkill_NameAsc(user.getId());
        InterviewGenerationContext context = new InterviewGenerationContext(
                request.interviewType(), request.difficulty(), request.questionCount(), targetRole,
                profile.getHeadline(), profile.getEducationLevel(), profile.getInstitution(),
                profile.getGraduationYear(), profile.getYearsOfExperience(), profile.getBio(),
                targetCompanyRepository.findAllByUser_IdOrderByCompanyNameAsc(user.getId())
                        .stream().map(TargetCompanyEntity::getCompanyName).toList(),
                userSkills.stream().map(this::skillDescription).toList());

        QuestionGenerationResult generated = questionGenerator.generate(context);
        if (generated.questions() == null || generated.questions().size() != request.questionCount()) {
            throw new InterviewConfigurationException("AI returned an unexpected number of questions");
        }

        InterviewSessionEntity session = sessionRepository.saveAndFlush(new InterviewSessionEntity(
                user, request.interviewType(), targetRole, request.difficulty(),
                generated.questions().size(), generated.model(), generated.promptVersion()));
        List<InterviewQuestionEntity> questions = new ArrayList<>();
        for (int index = 0; index < generated.questions().size(); index++) {
            GeneratedQuestion question = generated.questions().get(index);
            questions.add(new InterviewQuestionEntity(
                    session,
                    question.questionText(),
                    question.questionType(),
                    request.difficulty(),
                    writeTopics(question.expectedTopics()),
                    index + 1));
        }
        questionRepository.saveAll(questions);
        return response(session, questions);
    }

    @Transactional(readOnly = true)
    public List<InterviewSessionSummaryResponse> list(String email) {
        UserEntity user = currentUser(email);
        return sessionRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(InterviewSessionSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse get(String email, Long sessionId) {
        UserEntity user = currentUser(email);
        InterviewSessionEntity session = sessionRepository.findByIdAndUser_Id(sessionId, user.getId())
                .orElseThrow(InterviewNotFoundException::new);
        return response(session, questionRepository
                .findAllBySession_IdOrderBySequenceNumberAsc(session.getId()));
    }

    private InterviewSessionResponse response(
            InterviewSessionEntity session,
            List<InterviewQuestionEntity> questions) {
        List<InterviewQuestionResponse> questionResponses = questions.stream()
                .map(question -> new InterviewQuestionResponse(
                        question.getId(), question.getSequenceNumber(), question.getQuestionText(),
                        question.getQuestionType(), question.getDifficulty(),
                        readTopics(question.getExpectedTopicsJson()), question.isAiGenerated()))
                .toList();
        return InterviewSessionResponse.from(session, questionResponses);
    }

    private String skillDescription(UserSkillEntity skill) {
        return "%s (%s, %s years)".formatted(
                skill.getSkill().getName(), skill.getProficiency(), skill.getYearsUsed());
    }

    private String writeTopics(List<String> topics) {
        try {
            return objectMapper.writeValueAsString(topics == null ? List.of() : topics);
        } catch (Exception exception) {
            throw new IllegalStateException("Expected topics could not be stored", exception);
        }
    }

    private List<String> readTopics(String json) {
        try {
            JsonNode root = objectMapper.readTree(json == null ? "[]" : json);
            List<String> topics = new ArrayList<>();
            root.forEach(node -> topics.add(node.asText()));
            return List.copyOf(topics);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored expected topics are invalid", exception);
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) return preferred.trim();
        if (fallback != null && !fallback.isBlank()) return fallback.trim();
        return null;
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
