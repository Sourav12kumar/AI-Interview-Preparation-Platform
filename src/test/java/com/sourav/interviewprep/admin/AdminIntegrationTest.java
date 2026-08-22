package com.sourav.interviewprep.admin;

import com.sourav.interviewprep.admin.audit.AuditEventRepository;
import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.coding.repository.CodingProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CodingProblemRepository codingProblemRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_USER", "Platform user")));
        roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_ADMIN", "Platform administrator")));
    }

    @Test
    void enforcesAdminRoleAndAuditsUserAndProblemManagement() throws Exception {
        JsonNode candidateRegistration = register("Candidate", "candidate-admin-test@example.test");
        String candidateAccess = candidateRegistration.get("accessToken").asText();
        String candidateRefresh = candidateRegistration.get("refreshToken").asText();
        Long candidateId = userRepository.findByEmailIgnoreCase("candidate-admin-test@example.test")
                .orElseThrow().getId();

        register("Administrator", "administrator-test@example.test");
        UserEntity administrator = userRepository.findByEmailIgnoreCase("administrator-test@example.test")
                .orElseThrow();
        administrator.addRole(roleRepository.findByName("ROLE_ADMIN").orElseThrow());
        userRepository.saveAndFlush(administrator);
        String adminAccess = login("administrator-test@example.test");

        mockMvc.perform(get("/api/v1/admin/overview").header("Authorization", bearer(candidateAccess)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/overview").header("Authorization", bearer(adminAccess)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", candidateId)
                        .header("Authorization", bearer(adminAccess))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(candidateRefresh)))
                .andExpect(status().isUnauthorized());

        String problemJson = """
                {
                  "title":"Sum Two Values",
                  "slug":"sum-two-values-admin-test",
                  "description":"Return the sum of the two supplied integer values.",
                  "difficulty":"EASY",
                  "starterCode":{"JAVA":"class Solution { int sum(int a, int b) { return 0; } }"},
                  "testCases":[{"input":{"a":2,"b":3},"expected":5}],
                  "tags":["arrays"],
                  "active":true
                }
                """;
        String problemResponse = mockMvc.perform(post("/api/v1/admin/coding/problems")
                        .header("Authorization", bearer(adminAccess))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(problemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("sum-two-values-admin-test"))
                .andReturn().getResponse().getContentAsString();
        long problemId = objectMapper.readTree(problemResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/coding/problems")
                        .header("Authorization", bearer(adminAccess))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(problemJson))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/admin/coding/problems/{problemId}", problemId)
                        .header("Authorization", bearer(adminAccess)))
                .andExpect(status().isNoContent());

        assertThat(codingProblemRepository.findById(problemId).orElseThrow().isActive()).isFalse();
        assertThat(auditEventRepository.count()).isEqualTo(3);
        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .header("Authorization", bearer(adminAccess)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CODING_PROBLEM_DEACTIVATED"));
    }

    @Test
    void preventsAnAdministratorFromSuspendingTheirOwnAccount() throws Exception {
        register("Administrator", "self-admin-test@example.test");
        UserEntity administrator = userRepository.findByEmailIgnoreCase("self-admin-test@example.test")
                .orElseThrow();
        administrator.addRole(roleRepository.findByName("ROLE_ADMIN").orElseThrow());
        userRepository.saveAndFlush(administrator);
        String adminAccess = login("self-admin-test@example.test");

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", administrator.getId())
                        .header("Authorization", bearer(adminAccess))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode register(String fullName, String email) throws Exception {
        String request = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("fullName", fullName)
                .put("email", email)
                .put("password", "StrongPassword123!"));
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String login(String email) throws Exception {
        String request = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("email", email)
                .put("password", "StrongPassword123!"));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String refreshBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("refreshToken", refreshToken));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
