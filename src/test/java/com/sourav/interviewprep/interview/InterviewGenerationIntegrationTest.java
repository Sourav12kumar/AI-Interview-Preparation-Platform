package com.sourav.interviewprep.interview;

import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.interview.ai.GeneratedQuestion;
import com.sourav.interviewprep.interview.ai.InterviewQuestionGenerator;
import com.sourav.interviewprep.interview.ai.QuestionGenerationResult;
import com.sourav.interviewprep.interview.entity.QuestionType;
import com.sourav.interviewprep.interview.repository.InterviewQuestionRepository;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import com.sourav.interviewprep.evaluation.repository.AnswerEvaluationRepository;
import com.sourav.interviewprep.evaluation.repository.InterviewAnswerRepository;
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
import tools.jackson.databind.ObjectMapper;

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
@Import(InterviewGenerationIntegrationTest.FakeGeminiConfig.class)
class InterviewGenerationIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private InterviewQuestionRepository questionRepository;
    @Autowired private InterviewSessionRepository sessionRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private TargetCompanyRepository targetCompanyRepository;
    @Autowired private CandidateProfileRepository profileRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AnswerEvaluationRepository answerEvaluationRepository;
    @Autowired private InterviewAnswerRepository interviewAnswerRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        answerEvaluationRepository.deleteAll();
        interviewAnswerRepository.deleteAll();
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
    void shouldGeneratePersistAndReadOwnedInterview() throws Exception {
        String firstToken = registerAndGetAccessToken("first-interview@example.com");
        createProfile(firstToken);
        addSkill(firstToken);

        String created = mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewType":"TECHNICAL",
                                  "difficulty":"MEDIUM",
                                  "questionCount":3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetRole").value("Java Backend Developer"))
                .andExpect(jsonPath("$.totalQuestions").value(3))
                .andExpect(jsonPath("$.aiModel").value("fake-gemini"))
                .andExpect(jsonPath("$.promptVersion").value("test-v1"))
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0].expectedTopics[0]").value("Java"))
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/v1/interviews")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId))
                .andExpect(jsonPath("$[0].totalQuestions").value(3));

        mockMvc.perform(get("/api/v1/interviews/{sessionId}", sessionId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[2].sequenceNumber").value(3));

        String secondToken = registerAndGetAccessToken("second-interview@example.com");
        mockMvc.perform(get("/api/v1/interviews/{sessionId}", sessionId)
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireAuthenticationValidRequestAndCandidateProfile() throws Exception {
        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validInterviewBody()))
                .andExpect(status().isUnauthorized());

        String token = registerAndGetAccessToken("no-profile@example.com");
        mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validInterviewBody()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interviewType":"NOT_REAL",
                                  "difficulty":"MEDIUM",
                                  "questionCount":11
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private void createProfile(String token) throws Exception {
        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "headline":"Spring Boot Developer",
                                  "educationLevel":"B.Tech CSE",
                                  "institution":"Example College",
                                  "graduationYear":2025,
                                  "yearsOfExperience":2.0,
                                  "targetRole":"Java Backend Developer",
                                  "targetCompanies":["Microsoft","Google"],
                                  "bio":"Building secure REST APIs with Java."
                                }
                                """))
                .andExpect(status().isOk());
    }

    private void addSkill(String token) throws Exception {
        mockMvc.perform(post("/api/v1/profile/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Java",
                                  "category":"Programming Language",
                                  "proficiency":"ADVANCED",
                                  "yearsUsed":4.0
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("fullName", "Interview Candidate")
                .put("email", email)
                .put("password", "StrongPassword123!"));
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String validInterviewBody() {
        return """
                {"interviewType":"TECHNICAL","difficulty":"MEDIUM","questionCount":3}
                """;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeGeminiConfig {
        @Bean
        @Primary
        InterviewQuestionGenerator fakeQuestionGenerator() {
            return context -> new QuestionGenerationResult(
                    "fake-gemini",
                    "test-v1",
                    IntStream.rangeClosed(1, context.questionCount())
                            .mapToObj(number -> new GeneratedQuestion(
                                    "Generated Java interview question " + number + "?",
                                    QuestionType.TECHNICAL,
                                    List.of("Java", "Spring Boot")))
                            .toList());
        }
    }
}
