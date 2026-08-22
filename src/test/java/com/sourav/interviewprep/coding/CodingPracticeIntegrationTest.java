package com.sourav.interviewprep.coding;

import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.coding.entity.CodingDifficulty;
import com.sourav.interviewprep.coding.entity.CodingProblemEntity;
import com.sourav.interviewprep.coding.entity.SubmissionVerdict;
import com.sourav.interviewprep.coding.exception.CodeRunnerUnavailableException;
import com.sourav.interviewprep.coding.repository.CodingProblemRepository;
import com.sourav.interviewprep.coding.repository.CodingSubmissionRepository;
import com.sourav.interviewprep.coding.runner.CodeExecutionRequest;
import com.sourav.interviewprep.coding.runner.CodeExecutionResult;
import com.sourav.interviewprep.coding.runner.CodeRunner;
import com.sourav.interviewprep.evaluation.repository.AnswerEvaluationRepository;
import com.sourav.interviewprep.evaluation.repository.InterviewAnswerRepository;
import com.sourav.interviewprep.interview.repository.InterviewQuestionRepository;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import com.sourav.interviewprep.profile.repository.CandidateProfileRepository;
import com.sourav.interviewprep.profile.repository.SkillRepository;
import com.sourav.interviewprep.profile.repository.TargetCompanyRepository;
import com.sourav.interviewprep.profile.repository.UserSkillRepository;
import com.sourav.interviewprep.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(CodingPracticeIntegrationTest.FakeRunnerConfig.class)
class CodingPracticeIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RecordingCodeRunner codeRunner;
    @Autowired private CodingSubmissionRepository submissionRepository;
    @Autowired private CodingProblemRepository problemRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private AnswerEvaluationRepository answerEvaluationRepository;
    @Autowired private InterviewAnswerRepository interviewAnswerRepository;
    @Autowired private InterviewQuestionRepository interviewQuestionRepository;
    @Autowired private InterviewSessionRepository interviewSessionRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private TargetCompanyRepository targetCompanyRepository;
    @Autowired private CandidateProfileRepository profileRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private MockMvc mockMvc;
    private CodingProblemEntity activeProblem;
    private CodingProblemEntity inactiveProblem;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        submissionRepository.deleteAll();
        problemRepository.deleteAll();
        resumeRepository.deleteAll();
        answerEvaluationRepository.deleteAll();
        interviewAnswerRepository.deleteAll();
        interviewQuestionRepository.deleteAll();
        interviewSessionRepository.deleteAll();
        userSkillRepository.deleteAll();
        targetCompanyRepository.deleteAll();
        profileRepository.deleteAll();
        skillRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(new RoleEntity("ROLE_USER", "Platform user"));
        codeRunner.reset();

        activeProblem = problemRepository.save(new CodingProblemEntity(
                "Two Sum", "two-sum-test", "Return two matching indices.", CodingDifficulty.EASY,
                "{\"JAVA\":\"class Solution {}\"}",
                "[{\"input\":{\"nums\":[2,7],\"target\":9},\"expected\":[0,1]},"
                        + "{\"input\":{\"nums\":[3,3],\"target\":6},\"expected\":[0,1]}]",
                "[\"arrays\",\"hash-map\"]", true));
        inactiveProblem = problemRepository.save(new CodingProblemEntity(
                "Internal Problem", "internal-test", "Not published.", CodingDifficulty.HARD,
                "{}", "[{\"input\":1,\"expected\":1}]", "[\"internal\"]", false));
    }

    @AfterEach
    void cleanCodingData() {
        submissionRepository.deleteAll();
        problemRepository.deleteAll();
    }

    @Test
    void shouldListProblemsWithoutExposingHiddenTests() throws Exception {
        String token = registerAndGetAccessToken("catalogue@example.com");

        mockMvc.perform(get("/api/v1/coding/problems")
                        .param("difficulty", "EASY")
                        .param("tag", "ARRAYS")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activeProblem.getId()))
                .andExpect(jsonPath("$[0].tags[0]").value("arrays"))
                .andExpect(jsonPath("$[0].testCases").doesNotExist());

        mockMvc.perform(get("/api/v1/coding/problems/{problemId}", activeProblem.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.starterCode.JAVA").value("class Solution {}"))
                .andExpect(jsonPath("$.testCases").doesNotExist());

        mockMvc.perform(get("/api/v1/coding/problems/{problemId}", inactiveProblem.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSubmitCodeAndReturnOnlyOwnedHistory() throws Exception {
        String ownerToken = registerAndGetAccessToken("coder@example.com");
        String response = mockMvc.perform(post(
                        "/api/v1/coding/problems/{problemId}/submissions", activeProblem.getId())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"JAVA\",\"sourceCode\":\"class Solution {}\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verdict").value("ACCEPTED"))
                .andExpect(jsonPath("$.passedTestCases").value(2))
                .andExpect(jsonPath("$.totalTestCases").value(2))
                .andExpect(jsonPath("$.score").value(100.0))
                .andExpect(jsonPath("$.message").value("All tests passed"))
                .andReturn().getResponse().getContentAsString();
        long submissionId = objectMapper.readTree(response).get("id").asLong();

        assertThat(codeRunner.lastRequest).isNotNull();
        assertThat(codeRunner.lastRequest.problemSlug()).isEqualTo("two-sum-test");
        assertThat(codeRunner.lastRequest.testCases().size()).isEqualTo(2);
        assertThat(codeRunner.lastRequest.timeLimitMs()).isEqualTo(2_000);
        assertThat(codeRunner.lastRequest.memoryLimitMb()).isEqualTo(256);

        mockMvc.perform(get("/api/v1/coding/submissions")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(submissionId));

        mockMvc.perform(get("/api/v1/coding/submissions/{submissionId}", submissionId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceCode").value("class Solution {}"));

        String otherToken = registerAndGetAccessToken("other-coder@example.com");
        mockMvc.perform(get("/api/v1/coding/submissions")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/coding/submissions/{submissionId}", submissionId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnonymousInvalidAndUnavailableRunnerRequests() throws Exception {
        mockMvc.perform(post("/api/v1/coding/problems/{problemId}/submissions", activeProblem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"JAVA\",\"sourceCode\":\"class Solution {}\"}"))
                .andExpect(status().isUnauthorized());

        String token = registerAndGetAccessToken("validation@example.com");
        mockMvc.perform(post("/api/v1/coding/problems/{problemId}/submissions", activeProblem.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"RUBY\",\"sourceCode\":\"puts 1\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/coding/problems/{problemId}/submissions", activeProblem.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"JAVA\",\"sourceCode\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/coding/problems/{problemId}/submissions", inactiveProblem.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"JAVA\",\"sourceCode\":\"class Solution {}\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/coding/problems/{problemId}/submissions", activeProblem.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"JAVA\",\"sourceCode\":\"runner-down\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(submissionRepository.count()).isZero();
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

    private String bearer(String token) {
        return "Bearer " + token;
    }

    static final class RecordingCodeRunner implements CodeRunner {
        private CodeExecutionRequest lastRequest;

        @Override
        public CodeExecutionResult execute(CodeExecutionRequest request) {
            lastRequest = request;
            if ("runner-down".equals(request.sourceCode())) {
                throw new CodeRunnerUnavailableException("Code runner is unavailable");
            }
            return new CodeExecutionResult(
                    SubmissionVerdict.ACCEPTED,
                    request.testCases().size(),
                    request.testCases().size(),
                    42L,
                    2048L,
                    new BigDecimal("100.00"),
                    "All tests passed");
        }

        void reset() {
            lastRequest = null;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeRunnerConfig {
        @Bean
        @Primary
        RecordingCodeRunner recordingCodeRunner() {
            return new RecordingCodeRunner();
        }
    }
}
