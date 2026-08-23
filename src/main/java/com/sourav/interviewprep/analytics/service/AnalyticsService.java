package com.sourav.interviewprep.analytics.service;

import com.sourav.interviewprep.analytics.ai.PerformanceAdviceContext;
import com.sourav.interviewprep.analytics.ai.PerformanceAdviceResult;
import com.sourav.interviewprep.analytics.ai.PerformanceAdvisor;
import com.sourav.interviewprep.analytics.dto.AnalyticsDashboardResponse;
import com.sourav.interviewprep.analytics.dto.AnalyticsOverviewResponse;
import com.sourav.interviewprep.analytics.dto.AnalyticsPeriodResponse;
import com.sourav.interviewprep.analytics.dto.AnalyticsTrendPointResponse;
import com.sourav.interviewprep.analytics.dto.GeneratePerformanceReportRequest;
import com.sourav.interviewprep.analytics.dto.PerformanceReportResponse;
import com.sourav.interviewprep.analytics.dto.PerformanceReportSummaryResponse;
import com.sourav.interviewprep.analytics.dto.TopicPerformanceResponse;
import com.sourav.interviewprep.analytics.entity.PerformanceReportEntity;
import com.sourav.interviewprep.analytics.entity.TopicSource;
import com.sourav.interviewprep.analytics.exception.AnalyticsConfigurationException;
import com.sourav.interviewprep.analytics.exception.PerformanceReportNotFoundException;
import com.sourav.interviewprep.analytics.repository.PerformanceReportRepository;
import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.coding.entity.CodingSubmissionEntity;
import com.sourav.interviewprep.coding.entity.SubmissionVerdict;
import com.sourav.interviewprep.coding.repository.CodingSubmissionRepository;
import com.sourav.interviewprep.evaluation.entity.AnswerEvaluationEntity;
import com.sourav.interviewprep.evaluation.repository.AnswerEvaluationRepository;
import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import com.sourav.interviewprep.interview.entity.InterviewStatus;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AnalyticsService {

    private static final int DEFAULT_PERIOD_DAYS = 30;
    private static final int MAX_PERIOD_DAYS = 366;
    private static final int TOPIC_LIMIT = 5;

    private final UserRepository userRepository;
    private final InterviewSessionRepository sessionRepository;
    private final AnswerEvaluationRepository evaluationRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final PerformanceReportRepository reportRepository;
    private final PerformanceAdvisor performanceAdvisor;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AnalyticsService(
            UserRepository userRepository,
            InterviewSessionRepository sessionRepository,
            AnswerEvaluationRepository evaluationRepository,
            CodingSubmissionRepository submissionRepository,
            PerformanceReportRepository reportRepository,
            PerformanceAdvisor performanceAdvisor,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.evaluationRepository = evaluationRepository;
        this.submissionRepository = submissionRepository;
        this.reportRepository = reportRepository;
        this.performanceAdvisor = performanceAdvisor;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    public AnalyticsDashboardResponse dashboard(String email, LocalDate from, LocalDate to) {
        UserEntity user = currentUser(email);
        AnalyticsPeriod period = normalizePeriod(from, to);
        return aggregate(user.getId(), period);
    }

    public PerformanceReportResponse generateReport(
            String email,
            GeneratePerformanceReportRequest request) {
        UserEntity user = currentUser(email);
        AnalyticsPeriod period = validatePeriod(request.periodStart(), request.periodEnd());
        AnalyticsDashboardResponse dashboard = aggregate(user.getId(), period);
        if (dashboard.overview().interviewsCompleted() == 0
                && dashboard.overview().codingSubmissions() == 0) {
            throw new AnalyticsConfigurationException(
                    "Complete an interview or submit code before generating a report");
        }

        PerformanceAdviceResult advice = performanceAdvisor.advise(new PerformanceAdviceContext(
                period.from(), period.to(),
                dashboard.overview().interviewsCompleted(),
                dashboard.overview().codingProblemsAttempted(),
                dashboard.overview().codingSubmissions(),
                dashboard.overview().averageInterviewScore(),
                dashboard.overview().averageCodingScore(),
                dashboard.strongestTopics(),
                dashboard.improvementTopics()));

        PerformanceReportEntity report = reportRepository
                .findByUser_IdAndPeriodStartAndPeriodEnd(user.getId(), period.from(), period.to())
                .orElseGet(() -> new PerformanceReportEntity(user, period.from(), period.to()));
        report.refresh(
                dashboard.overview().interviewsCompleted(),
                dashboard.overview().codingProblemsAttempted(),
                dashboard.overview().averageInterviewScore(),
                dashboard.overview().averageCodingScore(),
                writeJson(dashboard.strongestTopics()),
                writeJson(dashboard.improvementTopics()),
                writeJson(advice.recommendations()),
                advice.summary(), advice.model(), advice.promptVersion());
        return reportResponse(reportRepository.saveAndFlush(report));
    }

    @Transactional(readOnly = true)
    public List<PerformanceReportSummaryResponse> listReports(String email) {
        UserEntity user = currentUser(email);
        return reportRepository.findAllByUser_IdOrderByGeneratedAtDesc(user.getId()).stream()
                .map(this::reportSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PerformanceReportResponse getReport(String email, Long reportId) {
        UserEntity user = currentUser(email);
        PerformanceReportEntity report = reportRepository.findByIdAndUser_Id(reportId, user.getId())
                .orElseThrow(PerformanceReportNotFoundException::new);
        return reportResponse(report);
    }

    private AnalyticsDashboardResponse aggregate(Long userId, AnalyticsPeriod period) {
        Instant from = period.from().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = period.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<InterviewSessionEntity> sessions = sessionRepository.findCompletedInPeriod(
                userId, InterviewStatus.COMPLETED, from, toExclusive);
        List<AnswerEvaluationEntity> evaluations = evaluationRepository.findAllInPeriod(
                userId, from, toExclusive);
        List<CodingSubmissionEntity> submissions = submissionRepository.findAllInPeriod(
                userId, from, toExclusive);

        BigDecimal averageInterviewScore = average(sessions.stream()
                .map(InterviewSessionEntity::getOverallScore)
                .toList());
        BigDecimal averageCodingScore = average(submissions.stream()
                .map(CodingSubmissionEntity::getScore)
                .toList());
        int acceptedSubmissions = (int) submissions.stream()
                .filter(value -> value.getVerdict() == SubmissionVerdict.ACCEPTED)
                .count();
        Set<Long> problemIds = new LinkedHashSet<>();
        submissions.forEach(value -> problemIds.add(value.getProblem().getId()));

        AnalyticsOverviewResponse overview = new AnalyticsOverviewResponse(
                sessions.size(), problemIds.size(), submissions.size(), acceptedSubmissions,
                averageInterviewScore, averageCodingScore,
                percentage(acceptedSubmissions, submissions.size()));

        Map<LocalDate, DailyAccumulator> daily = new LinkedHashMap<>();
        sessions.forEach(session -> {
            if (session.getCompletedAt() != null && session.getOverallScore() != null) {
                daily.computeIfAbsent(utcDate(session.getCompletedAt()), ignored -> new DailyAccumulator())
                        .addInterview(session.getOverallScore());
            }
        });
        submissions.forEach(submission -> daily
                .computeIfAbsent(utcDate(submission.getSubmittedAt()), ignored -> new DailyAccumulator())
                .addCoding(submission.getScore()));
        List<AnalyticsTrendPointResponse> trends = daily.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().response(entry.getKey()))
                .toList();

        Map<String, TopicAccumulator> topics = new LinkedHashMap<>();
        evaluations.forEach(evaluation -> readTextList(
                evaluation.getAnswer().getQuestion().getExpectedTopicsJson()).forEach(topic ->
                addTopic(topics, topic, TopicSource.INTERVIEW, evaluation.getOverallScore())));
        submissions.forEach(submission -> readTextList(submission.getProblem().getTags()).forEach(topic ->
                addTopic(topics, topic, TopicSource.CODING, submission.getScore())));
        List<TopicPerformanceResponse> topicScores = topics.values().stream()
                .map(TopicAccumulator::response)
                .toList();
        List<TopicPerformanceResponse> strongest = topicScores.stream()
                .sorted(Comparator.comparing(TopicPerformanceResponse::averageScore).reversed()
                        .thenComparing(TopicPerformanceResponse::attempts, Comparator.reverseOrder())
                        .thenComparing(TopicPerformanceResponse::topic))
                .limit(TOPIC_LIMIT)
                .toList();
        List<TopicPerformanceResponse> improvement = topicScores.stream()
                .sorted(Comparator.comparing(TopicPerformanceResponse::averageScore)
                        .thenComparing(TopicPerformanceResponse::attempts, Comparator.reverseOrder())
                        .thenComparing(TopicPerformanceResponse::topic))
                .limit(TOPIC_LIMIT)
                .toList();

        return new AnalyticsDashboardResponse(
                new AnalyticsPeriodResponse(period.from(), period.to()),
                overview, trends, strongest, improvement);
    }

    private AnalyticsPeriod normalizePeriod(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate resolvedTo = to == null ? today : to;
        LocalDate resolvedFrom = from == null
                ? resolvedTo.minusDays(DEFAULT_PERIOD_DAYS - 1L)
                : from;
        return validatePeriod(resolvedFrom, resolvedTo);
    }

    private AnalyticsPeriod validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new AnalyticsConfigurationException("Analytics period start and end are required");
        }
        if (from.isAfter(to)) {
            throw new AnalyticsConfigurationException("Analytics period start must not be after its end");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_PERIOD_DAYS) {
            throw new AnalyticsConfigurationException("Analytics period must not exceed 366 days");
        }
        if (to.isAfter(LocalDate.now(clock))) {
            throw new AnalyticsConfigurationException("Analytics period must not end in the future");
        }
        return new AnalyticsPeriod(from, to);
    }

    private PerformanceReportSummaryResponse reportSummary(PerformanceReportEntity report) {
        return new PerformanceReportSummaryResponse(
                report.getId(), report.getPeriodStart(), report.getPeriodEnd(),
                report.getInterviewsCompleted(), report.getCodingProblemsAttempted(),
                report.getAverageInterviewScore(), report.getAverageCodingScore(),
                report.getGeneratedAt());
    }

    private PerformanceReportResponse reportResponse(PerformanceReportEntity report) {
        return new PerformanceReportResponse(
                report.getId(), report.getPeriodStart(), report.getPeriodEnd(),
                report.getInterviewsCompleted(), report.getCodingProblemsAttempted(),
                report.getAverageInterviewScore(), report.getAverageCodingScore(),
                readTopics(report.getStrongestTopics()), readTopics(report.getImprovementTopics()),
                report.getAnalysisSummary(), readTextList(report.getAiRecommendations()),
                report.getAiModel(), report.getPromptVersion(), report.getGeneratedAt());
    }

    private void addTopic(
            Map<String, TopicAccumulator> topics,
            String rawTopic,
            TopicSource source,
            BigDecimal score) {
        if (rawTopic == null || rawTopic.isBlank() || score == null) return;
        String display = rawTopic.trim();
        String key = display.toLowerCase(Locale.ROOT);
        topics.computeIfAbsent(key, ignored -> new TopicAccumulator(display, source)).add(source, score);
    }

    private BigDecimal average(List<BigDecimal> scores) {
        List<BigDecimal> valid = scores.stream().filter(value -> value != null).toList();
        if (valid.isEmpty()) return null;
        BigDecimal total = valid.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(valid.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(int numerator, int denominator) {
        if (denominator == 0) return null;
        return BigDecimal.valueOf(numerator)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private LocalDate utcDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private List<String> readTextList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) throw new IllegalStateException();
            List<String> values = new ArrayList<>();
            root.forEach(node -> {
                String value = node.asText("").trim();
                if (!value.isBlank()) values.add(value);
            });
            return List.copyOf(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored analytics JSON is invalid", exception);
        }
    }

    private List<TopicPerformanceResponse> readTopics(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) throw new IllegalStateException();
            List<TopicPerformanceResponse> values = new ArrayList<>();
            root.forEach(node -> values.add(new TopicPerformanceResponse(
                    node.path("topic").asText(),
                    TopicSource.valueOf(node.path("source").asText()),
                    node.path("attempts").asInt(),
                    node.path("averageScore").decimalValue())));
            return List.copyOf(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored analytics topics are invalid", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Analytics report could not be stored", exception);
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

    private record AnalyticsPeriod(LocalDate from, LocalDate to) {
    }

    private static final class DailyAccumulator {
        private BigDecimal interviewTotal = BigDecimal.ZERO;
        private BigDecimal codingTotal = BigDecimal.ZERO;
        private int interviewCount;
        private int codingCount;

        void addInterview(BigDecimal score) {
            interviewTotal = interviewTotal.add(score);
            interviewCount++;
        }

        void addCoding(BigDecimal score) {
            if (score == null) return;
            codingTotal = codingTotal.add(score);
            codingCount++;
        }

        AnalyticsTrendPointResponse response(LocalDate date) {
            return new AnalyticsTrendPointResponse(
                    date,
                    average(interviewTotal, interviewCount),
                    average(codingTotal, codingCount),
                    interviewCount,
                    codingCount);
        }

        private BigDecimal average(BigDecimal total, int count) {
            if (count == 0) return null;
            return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
    }

    private static final class TopicAccumulator {
        private final String topic;
        private TopicSource source;
        private BigDecimal total = BigDecimal.ZERO;
        private int attempts;

        TopicAccumulator(String topic, TopicSource source) {
            this.topic = topic;
            this.source = source;
        }

        void add(TopicSource addedSource, BigDecimal score) {
            if (source != addedSource) source = TopicSource.MIXED;
            total = total.add(score);
            attempts++;
        }

        TopicPerformanceResponse response() {
            return new TopicPerformanceResponse(
                    topic, source, attempts,
                    total.divide(BigDecimal.valueOf(attempts), 2, RoundingMode.HALF_UP));
        }
    }
}
