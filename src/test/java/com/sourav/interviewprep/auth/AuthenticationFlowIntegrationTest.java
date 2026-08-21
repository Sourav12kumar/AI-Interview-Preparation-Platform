package com.sourav.interviewprep.auth;

import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private TargetCompanyRepository targetCompanyRepository;

    @Autowired
    private CandidateProfileRepository profileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private InterviewQuestionRepository interviewQuestionRepository;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
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
    void shouldCompleteRegistrationRefreshAndLogoutLifecycle() throws Exception {
        String registerBody = """
                {
                  "fullName": "Sourav Kumbhakar",
                  "email": "SOURAV@example.com",
                  "password": "StrongPassword123!"
                }
                """;

        String registrationJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("sourav@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_USER"))
                .andReturn().getResponse().getContentAsString();

        JsonNode registration = objectMapper.readTree(registrationJson);
        String accessToken = registration.get("accessToken").asText();
        String firstRefreshToken = registration.get("refreshToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Sourav Kumbhakar"));

        String refreshJson = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(firstRefreshToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(refreshJson).get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(firstRefreshToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rotatedRefreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLoginAndRejectWrongPassword() throws Exception {
        registerDefaultUser();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sourav@example.com","password":"StrongPassword123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sourav@example.com","password":"incorrect-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectDuplicateEmailAndWeakRegistrationRequest() throws Exception {
        registerDefaultUser();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Duplicate","email":"SOURAV@example.com","password":"AnotherPassword123!"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test","email":"not-an-email","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void shouldRejectRefreshJwtAsApiAccessToken() throws Exception {
        String json = registerDefaultUser();
        String refreshToken = objectMapper.readTree(json).get("refreshToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    private String registerDefaultUser() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Sourav Kumbhakar",
                                  "email":"sourav@example.com",
                                  "password":"StrongPassword123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String refreshBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("refreshToken", refreshToken));
    }
}
