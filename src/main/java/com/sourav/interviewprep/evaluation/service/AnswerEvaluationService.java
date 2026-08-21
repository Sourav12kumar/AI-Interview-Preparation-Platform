package com.sourav.interviewprep.evaluation.service;

import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.evaluation.ai.AnswerEvaluationContext;
import com.sourav.interviewprep.evaluation.ai.AnswerEvaluationResult;
import com.sourav.interviewprep.evaluation.ai.AnswerEvaluator;
import com.sourav.interviewprep.evaluation.dto.AnswerEvaluationResponse;
import com.sourav.interviewprep.evaluation.dto.SubmitAnswerRequest;
import com.sourav.interviewprep.evaluation.dto.SubmittedAnswerResponse;
import com.sourav.interviewprep.evaluation.entity.AnswerEvaluationEntity;
import com.sourav.interviewprep.evaluation.entity.InterviewAnswerEntity;
import com.sourav.interviewprep.evaluation.exception.DuplicateAnswerException;
import com.sourav.interviewprep.evaluation.repository.AnswerEvaluationRepository;
import com.sourav.interviewprep.evaluation.repository.InterviewAnswerRepository;
import com.sourav.interviewprep.interview.entity.InterviewQuestionEntity;
import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import com.sourav.interviewprep.interview.entity.InterviewStatus;
import com.sourav.interviewprep.interview.exception.InterviewConfigurationException;
import com.sourav.interviewprep.interview.exception.InterviewNotFoundException;
import com.sourav.interviewprep.interview.repository.InterviewQuestionRepository;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnswerEvaluationService {

    private final UserRepository userRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final AnswerEvaluationRepository evaluationRepository;
    private final AnswerEvaluator answerEvaluator;
    private final ObjectMapper objectMapper;

    public AnswerEvaluationService(
            UserRepository userRepository,
            InterviewSessionRepository sessionRepository,
            InterviewQuestionRepository questionRepository,
            InterviewAnswerRepository answerRepository,
            AnswerEvaluationRepository evaluationRepository,
            AnswerEvaluator answerEvaluator,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.evaluationRepository = evaluationRepository;
        this.answerEvaluator = answerEvaluator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmittedAnswerResponse submit(
            String email,
            Long sessionId,
            SubmitAnswerRequest request) {
        UserEntity user = currentUser(email);
        InterviewSessionEntity session = sessionRepository.findOwnedForUpdate(sessionId, user.getId())
                .orElseThrow(InterviewNotFoundException::new);
        if (session.getStatus() == InterviewStatus.COMPLETED
                || session.getStatus() == InterviewStatus.CANCELLED) {
            throw new InterviewConfigurationException("This interview session no longer accepts answers");
        }
        InterviewQuestionEntity question = questionRepository
                .findByIdAndSession_Id(request.questionId(), session.getId())
                .orElseThrow(InterviewNotFoundException::new);
        if (answerRepository.existsByQuestion_Id(question.getId())) {
            throw new DuplicateAnswerException();
        }

        AnswerEvaluationResult result = answerEvaluator.evaluate(new AnswerEvaluationContext(
                session.getTargetRole(), question.getDifficulty(), question.getQuestionText(),
                question.getQuestionType(), readList(question.getExpectedTopicsJson()),
                request.answerText().trim()));

        InterviewAnswerEntity answer = answerRepository.saveAndFlush(new InterviewAnswerEntity(
                question, request.answerText(), request.responseTimeSeconds()));
        AnswerEvaluationEntity evaluation = evaluationRepository.saveAndFlush(new AnswerEvaluationEntity(
                answer,
                result.technicalScore(),
                result.relevanceScore(),
                result.clarityScore(),
                result.confidenceScore(),
                result.overallScore(),
                writeList(result.strengths()),
                writeList(result.improvements()),
                result.idealAnswer(),
                result.model()));

        long answeredQuestions = answerRepository.countByQuestion_Session_Id(session.getId());
        if (answeredQuestions == session.getTotalQuestions()) {
            session.complete(averageScore(session.getId()));
        } else {
            session.markInProgress();
        }
        return response(answer, evaluation);
    }

    @Transactional(readOnly = true)
    public List<SubmittedAnswerResponse> list(String email, Long sessionId) {
        UserEntity user = currentUser(email);
        ownedSession(sessionId, user.getId());
        return answerRepository.findAllByQuestion_Session_IdOrderByQuestion_SequenceNumberAsc(sessionId)
                .stream()
                .map(answer -> response(answer, evaluationRepository.findByAnswer_Id(answer.getId())
                        .orElseThrow(() -> new IllegalStateException("Stored answer has no evaluation"))))
                .toList();
    }

    private BigDecimal averageScore(Long sessionId) {
        List<AnswerEvaluationEntity> evaluations = evaluationRepository.findAllBySessionId(sessionId);
        BigDecimal total = evaluations.stream()
                .map(AnswerEvaluationEntity::getOverallScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(evaluations.size()), 2, RoundingMode.HALF_UP);
    }

    private SubmittedAnswerResponse response(
            InterviewAnswerEntity answer,
            AnswerEvaluationEntity evaluation) {
        AnswerEvaluationResponse details = new AnswerEvaluationResponse(
                evaluation.getId(),
                evaluation.getTechnicalScore(),
                evaluation.getRelevanceScore(),
                evaluation.getClarityScore(),
                evaluation.getConfidenceScore(),
                evaluation.getOverallScore(),
                readList(evaluation.getStrengths()),
                readList(evaluation.getImprovements()),
                evaluation.getIdealAnswer(),
                evaluation.getAiModel(),
                evaluation.getEvaluatedAt());
        return new SubmittedAnswerResponse(
                answer.getId(), answer.getQuestion().getId(), answer.getAnswerText(),
                answer.getResponseTimeSeconds(), answer.getSubmittedAt(), details);
    }

    private List<String> readList(String json) {
        try {
            JsonNode root = objectMapper.readTree(json == null ? "[]" : json);
            List<String> values = new ArrayList<>();
            root.forEach(node -> values.add(node.asText()));
            return List.copyOf(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored evaluation JSON is invalid", exception);
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            throw new IllegalStateException("Evaluation feedback could not be stored", exception);
        }
    }

    private InterviewSessionEntity ownedSession(Long sessionId, Long userId) {
        return sessionRepository.findByIdAndUser_Id(sessionId, userId)
                .orElseThrow(InterviewNotFoundException::new);
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
