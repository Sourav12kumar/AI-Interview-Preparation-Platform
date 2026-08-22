package com.sourav.interviewprep.analytics;

import com.sourav.interviewprep.analytics.ai.PerformanceAdviceContext;
import com.sourav.interviewprep.analytics.ai.PerformanceAdviceResult;
import com.sourav.interviewprep.analytics.ai.PerformanceAdvisor;
import com.sourav.interviewprep.analytics.repository.PerformanceReportRepository;
import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.coding.entity.CodingDifficulty;
import com.sourav.interviewprep.coding.entity.CodingProblemEntity;
import com.sourav.interviewprep.coding.entity.CodingSubmissionEntity;
import com.sourav.interviewprep.coding.entity.ProgrammingLanguage;
import com.sourav.interviewprep.coding.entity.SubmissionVerdict;
import com.sourav.interviewprep.coding.repository.CodingProblemRepository;
import com.sourav.interviewprep.coding.repository.CodingSubmissionRepository;
import com.sourav.interviewprep.coding.runner.CodeExecutionResult;
import com.sourav.interviewprep.evaluation.entity.AnswerEvaluationEntity;
import com.sourav.interviewprep.evaluation.entity.InterviewAnswerEntity;
import com.sourav.interviewprep.evaluation.repository.AnswerEvaluationRepository;
import com.sourav.interviewprep.evaluation.repository.InterviewAnswerRepository;
import com.sourav.interviewprep.interview.entity.Difficulty;
import com.sourav.interviewprep.interview.entity.InterviewQuestionEntity;
import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import com.sourav.interviewprep.interview.entity.InterviewType;
import com.sourav.interviewprep.interview.entity.QuestionType;
import com.sourav.interviewprep.interview.repository.InterviewQuestionRepository;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import com.sourav.interviewprep.profile.repository.CandidateProfileRepository;
import com.sourav.interviewprep.profile.repository.SkillRepository;
import com.sourav.interviewprep.profile.repository.TargetCompanyRepository;
import com.sourav.interviewprep.profile.repository.UserSkillRepository;
import com.sourav.interviewprep.resume.repository.ResumeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(AnalyticsIntegrationTest.FakeAdvisorConfig.class)
class AnalyticsIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RecordingPerformanceAdvisor performanceAdvisor;
    @Autowired private PerformanceReportRepository reportRepository;
    @Autowired private CodingSubmissionRepository submissionRepository;
    @Autowired private CodingProblemRepository problemRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private AnswerEvaluationRepository evaluationRepository;
    @Autowired private InterviewAnswerRepository answerRepository;
    @Autowired private InterviewQuestionRepository questionRepository;
    @Autowired private InterviewSessionRepository sessionRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private TargetCompanyRepository targetCompanyRepository;
    @Autowired private CandidateProfileRepository profileRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        cleanAll();
        roleRepository.save(new RoleEntity("ROLE_USER", "Platform user"));
        performanceAdvisor.lastContext = null;
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    @Test
    void shouldBuildDashboardGenerateReportAndProtectOwnership() throws Exception {
        String token = registerAndGetAccessToken("analytics-owner@example.com");
        UserEntity owner = userRepository.findByEmailIgnoreCase("analytics-owner@example.com").orElseThrow();
        seedPerformance(owner);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("from", today.minusDays(7).toString())
                        .param("to", today.toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.interviewsCompleted").value(1))
                .andExpect(jsonPath("$.overview.codingProblemsAttempted").value(1))
                .andExpect(jsonPath("$.overview.codingSubmissions").value(2))
                .andExpect(jsonPath("$.overview.acceptedSubmissions").value(1))
                .andExpect(jsonPath("$.overview.averageInterviewScore").value(80.0))
                .andExpect(jsonPath("$.overview.averageCodingScore").value(75.0))
                .andExpect(jsonPath("$.overview.codingAcceptanceRate").value(50.0))
                .andExpect(jsonPath("$.trends.length()").value(1))
                .andExpect(jsonPath("$.strongestTopics[0].topic").value("arrays"))
                .andExpect(jsonPath("$.improvementTopics[0].averageScore").value(70.0));

        String request = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("periodStart", today.minusDays(7).toString())
                .put("periodEnd", today.toString()));
        String generated = mockMvc.perform(post("/api/v1/analytics/reports")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Steady progress with a clear next focus."))
                .andExpect(jsonPath("$.recommendations.length()").value(3))
                .andExpect(jsonPath("$.aiModel").value("fake-gemini"))
                .andExpect(jsonPath("$.promptVersion").value("analytics-test-v1"))
                .andReturn().getResponse().getContentAsString();
        long reportId = objectMapper.readTree(generated).get("id").asLong();

        assertThat(performanceAdvisor.lastContext).isNotNull();
        assertThat(performanceAdvisor.lastContext.codingSubmissions()).isEqualTo(2);
        assertThat(reportRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/analytics/reports")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId));
        assertThat(reportRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/analytics/reports")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reportId));
        mockMvc.perform(get("/api/v1/analytics/reports/{reportId}", reportId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strongestTopics[0].source").value("CODING"));

        String otherToken = registerAndGetAccessToken("analytics-other@example.com");
        mockMvc.perform(get("/api/v1/analytics/reports/{reportId}", reportId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnEmptyDashboardAndRejectInvalidOrEmptyReports() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/dashboard"))
                .andExpect(status().isUnauthorized());

        String token = registerAndGetAccessToken("analytics-empty@example.com");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.interviewsCompleted").value(0))
                .andExpect(jsonPath("$.overview.averageInterviewScore").doesNotExist())
                .andExpect(jsonPath("$.trends.length()").value(0));

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("from", today.toString())
                        .param("to", today.minusDays(1).toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("to", today.plusDays(1).toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/analytics/reports")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodStart\":\"" + today.minusDays(7)
                                + "\",\"periodEnd\":\"" + today + "\"}"))
                .andExpect(status().isBadRequest());
        assertThat(reportRepository.count()).isZero();
    }

    private void seedPerformance(UserEntity user) {
        InterviewSessionEntity session = sessionRepository.save(new InterviewSessionEntity(
                user, InterviewType.TECHNICAL, "Backend Developer", Difficulty.MEDIUM,
                1, "fake-gemini", "test-v1"));
        InterviewQuestionEntity question = questionRepository.save(new InterviewQuestionEntity(
                session, "Explain REST in Spring.", QuestionType.TECHNICAL, Difficulty.MEDIUM,
                "[\"Spring\",\"REST\"]", 1));
        InterviewAnswerEntity answer = answerRepository.save(new InterviewAnswerEntity(
                question, "REST uses resources and HTTP methods.", 60));
        evaluationRepository.save(new AnswerEvaluationEntity(
                answer,
                new BigDecimal("70.00"), new BigDecimal("70.00"),
                new BigDecimal("70.00"), new BigDecimal("70.00"),
                new BigDecimal("70.00"), "[\"Clear\"]", "[\"Add examples\"]",
                "A complete ideal answer.", "fake-gemini"));
        session.complete(new BigDecimal("80.00"));
        sessionRepository.saveAndFlush(session);

        CodingProblemEntity problem = problemRepository.save(new CodingProblemEntity(
                "Array Practice", "analytics-array", "Solve the array task.", CodingDifficulty.EASY,
                "{}", "[{\"input\":1,\"expected\":1}]", "[\"arrays\"]", true));
        submissionRepository.save(new CodingSubmissionEntity(
                user, problem, ProgrammingLanguage.JAVA, "class Solution {}",
                new CodeExecutionResult(
                        SubmissionVerdict.ACCEPTED, 2, 2, 20L, 1024L,
                        new BigDecimal("100.00"), "Accepted")));
        submissionRepository.saveAndFlush(new CodingSubmissionEntity(
                user, problem, ProgrammingLanguage.PYTHON, "def solve(): pass",
                new CodeExecutionResult(
                        SubmissionVerdict.WRONG_ANSWER, 1, 2, 15L, 800L,
                        new BigDecimal("50.00"), "One test failed")));
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("fullName", "Candidate")
                .put("email", email)
                .put("password", "StrongPassword123!"));
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.get("accessToken").asText();
    }

    private void cleanAll() {
        reportRepository.deleteAll();
        submissionRepository.deleteAll();
        problemRepository.deleteAll();
        resumeRepository.deleteAll();
        evaluationRepository.deleteAll();
        answerRepository.deleteAll();
        questionRepository.deleteAll();
        sessionRepository.deleteAll();
        userSkillRepository.deleteAll();
        targetCompanyRepository.deleteAll();
        profileRepository.deleteAll();
        skillRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    static final class RecordingPerformanceAdvisor implements PerformanceAdvisor {
        private PerformanceAdviceContext lastContext;

        @Override
        public PerformanceAdviceResult advise(PerformanceAdviceContext context) {
            lastContext = context;
            return new PerformanceAdviceResult(
                    "Steady progress with a clear next focus.",
                    List.of(
                            "Practise Spring explanations with examples.",
                            "Solve two array problems under a timer.",
                            "Review mistakes before the next mock interview."),
                    "fake-gemini",
                    "analytics-test-v1");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeAdvisorConfig {
        @Bean
        @Primary
        RecordingPerformanceAdvisor recordingPerformanceAdvisor() {
            return new RecordingPerformanceAdvisor();
        }
    }
}
