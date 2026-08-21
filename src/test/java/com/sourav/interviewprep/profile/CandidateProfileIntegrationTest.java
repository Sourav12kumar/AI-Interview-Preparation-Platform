package com.sourav.interviewprep.profile;

import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class CandidateProfileIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private TargetCompanyRepository targetCompanyRepository;
    @Autowired private CandidateProfileRepository profileRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private InterviewQuestionRepository interviewQuestionRepository;
    @Autowired private InterviewSessionRepository interviewSessionRepository;
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
    void shouldManageOwnedProfileCompaniesAndSkills() throws Exception {
        String token = registerAndGetAccessToken("candidate@example.com");

        mockMvc.perform(get("/api/v1/profile").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Backend Engineer"))
                .andExpect(jsonPath("$.educationLevel").value("B.Tech"))
                .andExpect(jsonPath("$.targetRole").value("Senior Java Developer"))
                .andExpect(jsonPath("$.targetCompanies.length()").value(2));

        String skillJson = mockMvc.perform(post("/api/v1/profile/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Java",
                                  "category":"Programming Language",
                                  "proficiency":"ADVANCED",
                                  "yearsUsed":4.5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"))
                .andReturn().getResponse().getContentAsString();
        long skillId = objectMapper.readTree(skillJson).get("id").asLong();

        mockMvc.perform(get("/api/v1/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[0].name").value("Java"))
                .andExpect(jsonPath("$.skills[0].proficiency").value("ADVANCED"));

        mockMvc.perform(put("/api/v1/profile/skills/{skillId}", skillId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"proficiency":"EXPERT","yearsUsed":5.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proficiency").value("EXPERT"));

        mockMvc.perform(post("/api/v1/profile/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"java",
                                  "category":"Programming Language",
                                  "proficiency":"BEGINNER",
                                  "yearsUsed":1.0
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/profile").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/profile/skills").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectUnauthenticatedAndInvalidProfileRequests() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized());

        String token = registerAndGetAccessToken("validation@example.com");
        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "graduationYear":1800,
                                  "yearsOfExperience":-1,
                                  "targetCompanies":["   "]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPreventCrossUserSkillAccess() throws Exception {
        String firstToken = registerAndGetAccessToken("first@example.com");
        String skillJson = mockMvc.perform(post("/api/v1/profile/skills")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Spring Boot",
                                  "category":"Framework",
                                  "proficiency":"ADVANCED",
                                  "yearsUsed":3.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long skillId = objectMapper.readTree(skillJson).get("id").asLong();

        String secondToken = registerAndGetAccessToken("second@example.com");
        mockMvc.perform(put("/api/v1/profile/skills/{skillId}", skillId)
                        .header("Authorization", bearer(secondToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"proficiency":"EXPERT","yearsUsed":10.0}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/profile/skills/{skillId}", skillId)
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/profile/skills").header("Authorization", bearer(secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
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

    private String profileBody() {
        return """
                {
                  "headline":"Backend Engineer",
                  "phone":"+91-9999999999",
                  "location":"Kolkata, India",
                  "educationLevel":"B.Tech",
                  "institution":"Example Institute of Technology",
                  "graduationYear":2025,
                  "yearsOfExperience":2.5,
                  "targetRole":"Senior Java Developer",
                  "targetCompanies":["Google","OpenAI","google"],
                  "bio":"Java and Spring Boot developer preparing for product interviews."
                }
                """;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
