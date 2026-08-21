package com.sourav.interviewprep.resume;

import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.evaluation.repository.AnswerEvaluationRepository;
import com.sourav.interviewprep.evaluation.repository.InterviewAnswerRepository;
import com.sourav.interviewprep.interview.repository.InterviewQuestionRepository;
import com.sourav.interviewprep.interview.repository.InterviewSessionRepository;
import com.sourav.interviewprep.profile.repository.CandidateProfileRepository;
import com.sourav.interviewprep.profile.repository.SkillRepository;
import com.sourav.interviewprep.profile.repository.TargetCompanyRepository;
import com.sourav.interviewprep.profile.repository.UserSkillRepository;
import com.sourav.interviewprep.resume.ai.ResumeAnalysisResult;
import com.sourav.interviewprep.resume.ai.ResumeAnalyzer;
import com.sourav.interviewprep.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(ResumeIntegrationTest.FakeResumeAiConfig.class)
class ResumeIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
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
    }

    @Test
    void shouldUploadExtractAnalyzeListAndDeleteOwnedResume() throws Exception {
        String token = registerAndGetAccessToken("resume-owner@example.com");
        MockMultipartFile file = resumeFile("Sourav-Resume.txt");

        String uploaded = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("Sourav-Resume.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.analysisStatus").value("PENDING"))
                .andExpect(jsonPath("$.atsScore").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(uploaded).get("id").asLong();

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/analysis", resumeId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole":"Java Backend Developer",
                                  "jobDescription":"Build Spring Boot APIs using MySQL and Docker."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.targetRole").value("Java Backend Developer"))
                .andExpect(jsonPath("$.atsScore").value(84.0))
                .andExpect(jsonPath("$.strengths[0]").value("Relevant Java experience"))
                .andExpect(jsonPath("$.missingKeywords[0]").value("Docker"))
                .andExpect(jsonPath("$.aiModel").value("fake-gemini"))
                .andExpect(jsonPath("$.promptVersion").value("resume-test-v1"));

        mockMvc.perform(get("/api/v1/resumes").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(resumeId));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}", resumeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Strong backend foundation with measurable room to improve."));

        mockMvc.perform(delete("/api/v1/resumes/{resumeId}", resumeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/resumes/{resumeId}", resumeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnonymousInvalidAndCrossUserResumeAccess() throws Exception {
        mockMvc.perform(multipart("/api/v1/resumes").file(resumeFile("resume.txt")))
                .andExpect(status().isUnauthorized());

        String ownerToken = registerAndGetAccessToken("resume-first@example.com");
        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile(
                                "file", "resume.exe", "application/octet-stream",
                                "not a resume".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile(
                                "file", "fake.pdf", "application/pdf",
                                "plain text pretending to be a pdf".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest());

        String uploaded = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(resumeFile("owned.txt"))
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(uploaded).get("id").asLong();

        String otherToken = registerAndGetAccessToken("resume-second@example.com");
        mockMvc.perform(get("/api/v1/resumes/{resumeId}", resumeId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/resumes/{resumeId}/analysis", resumeId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRole\":\"Java Developer\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/resumes/{resumeId}", resumeId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireTargetRoleBeforeAnalysis() throws Exception {
        String token = registerAndGetAccessToken("resume-role@example.com");
        String uploaded = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(resumeFile("role.txt"))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(uploaded).get("id").asLong();

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/analysis", resumeId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private MockMultipartFile resumeFile(String filename) {
        String resume = """
                Sourav Kumbhakar
                Java Backend Developer
                Experience building REST APIs with Java, Spring Boot, MySQL, and GitHub Actions.
                Improved API response time by 30 percent and wrote integration tests.
                """;
        return new MockMultipartFile(
                "file", filename, "text/plain", resume.getBytes(StandardCharsets.UTF_8));
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

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeResumeAiConfig {
        @Bean
        @Primary
        ResumeAnalyzer fakeResumeAnalyzer() {
            return context -> new ResumeAnalysisResult(
                    new BigDecimal("84.00"),
                    "Strong backend foundation with measurable room to improve.",
                    List.of("Relevant Java experience"),
                    List.of("Few cloud deployment examples"),
                    List.of("Docker"),
                    List.of("Add a quantified Docker deployment project"),
                    "fake-gemini",
                    "resume-test-v1");
        }
    }
}
