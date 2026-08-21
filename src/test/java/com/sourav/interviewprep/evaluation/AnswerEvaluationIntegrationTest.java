package com.sourav.interviewprep.evaluation;

import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.evaluation.ai.AnswerEvaluationResult;
import com.sourav.interviewprep.evaluation.ai.AnswerEvaluator;
import com.sourav.interviewprep.evaluation.repository.AnswerEvaluationRepository;
import com.sourav.interviewprep.evaluation.repository.InterviewAnswerRepository;
import com.sourav.interviewprep.interview.ai.GeneratedQuestion;
import com.sourav.interviewprep.interview.ai.InterviewQuestionGenerator;
import com.sourav.interviewprep.interview.ai.QuestionGenerationResult;
import com.sourav.interviewprep.interview.entity.QuestionType;
import com.sourav.interviewprep.interview.repository.InterviewQuestionRepository;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import com.sourav.interviewprep.profile.repository.CandidateProfileRepository;
import com.sourav.interviewprep.profile.repository.SkillRepository;
import com.sourav.interviewprep.profile.repository.TargetCompanyRepository;
import com.sourav.interviewprep.profile.repository.UserSkillRepository;
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
import java.util.List;
import java.util.stream.IntStream;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(AnswerEvaluationIntegrationTest.FakeAiConfig.class)
class AnswerEvaluationIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
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
        roleRepository.save(new RoleEntity("ROLE_USER", "Platform user"));
    }

    @Test
    void shouldEvaluateAnswersAndCompleteInterviewWithAverageScore() throws Exception {
        String token = registerAndGetAccessToken("evaluation@example.com");
        createProfile(token);
        JsonNode interview = createInterview(token, 2);
        long sessionId = interview.get("id").asLong();
        long firstQuestionId = interview.path("questions").get(0).get("id").asLong();
        long secondQuestionId = interview.path("questions").get(1).get("id").asLong();

        mockMvc.perform(post("/api/v1/interviews/{sessionId}/answers", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody(firstQuestionId, "first answer")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluation.overallScore").value(80.0))
                .andExpect(jsonPath("$.evaluation.strengths[0]").value("Clear structure"));

        mockMvc.perform(get("/api/v1/interviews/{sessionId}", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/interviews/{sessionId}/answers", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody(firstQuestionId, "duplicate")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/interviews/{sessionId}/answers", sessionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody(secondQuestionId, "second answer")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluation.overallScore").value(90.0));

        mockMvc.perform(get("/api/v1/interviews/{sessionId}", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.overallScore").value(85.0))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/interviews/{sessionId}/answers", sessionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].evaluation.idealAnswer").isNotEmpty());
    }

    @Test
    void shouldRejectInvalidAnonymousAndCrossUserSubmissions() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner@example.com");
        createProfile(ownerToken);
        JsonNode interview = createInterview(ownerToken, 1);
        long sessionId = interview.get("id").asLong();
        long questionId = interview.path("questions").get(0).get("id").asLong();

        mockMvc.perform(post("/api/v1/interviews/{sessionId}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody(questionId, "answer")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/interviews/{sessionId}/answers", sessionId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody(questionId, "   ")))
                .andExpect(status().isBadRequest());

        String otherToken = registerAndGetAccessToken("other@example.com");
        mockMvc.perform(post("/api/v1/interviews/{sessionId}/answers", sessionId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody(questionId, "attempt")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/interviews/{sessionId}/answers", sessionId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    private JsonNode createInterview(String token, int questionCount) throws Exception {
        String response = mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewType":"TECHNICAL",
                                  "difficulty":"MEDIUM",
                                  "questionCount":%d
                                }
                                """.formatted(questionCount)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private void createProfile(String token) throws Exception {
        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "headline":"Java Developer",
                                  "yearsOfExperience":2.0,
                                  "targetRole":"Java Backend Developer",
                                  "targetCompanies":["Microsoft"]
                                }
                                """))
                .andExpect(status().isOk());
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
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String answerBody(long questionId, String answer) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("questionId", questionId)
                .put("answerText", answer)
                .put("responseTimeSeconds", 90));
    }

    private String bearer(String token) { return "Bearer " + token; }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeAiConfig {
        @Bean
        @Primary
        InterviewQuestionGenerator fakeQuestionGenerator() {
            return context -> new QuestionGenerationResult(
                    "fake-gemini", "test-v1",
                    IntStream.rangeClosed(1, context.questionCount())
                            .mapToObj(number -> new GeneratedQuestion(
                                    "Question " + number + "?", QuestionType.TECHNICAL,
                                    List.of("Java", "Spring Boot")))
                            .toList());
        }

        @Bean
        @Primary
        AnswerEvaluator fakeAnswerEvaluator() {
            return context -> {
                BigDecimal score = context.answerText().contains("second")
                        ? new BigDecimal("90.00") : new BigDecimal("80.00");
                return new AnswerEvaluationResult(
                        score, score, score, score, score,
                        List.of("Clear structure"),
                        List.of("Add a concrete example"),
                        "An ideal answer with reasoning and an example.",
                        "fake-gemini");
            };
        }
    }
}
