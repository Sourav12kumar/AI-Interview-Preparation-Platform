package com.sourav.interviewprep.web;

import com.sourav.interviewprep.auth.browser.BrowserRefreshCookieService;
import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FrontendIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new RoleEntity("ROLE_USER", "Platform user")));
    }

    @Test
    void rendersPublicFrontendPagesWithSecurityHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("InterviewPilot")))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Candidate profile")));
        mockMvc.perform(get("/js/profile.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/profile")));
        mockMvc.perform(get("/interviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("interviews"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mock interview studio")));
        mockMvc.perform(get("/interviews/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("interview-session"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Question map")));
        mockMvc.perform(get("/js/interview-session.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/answers")));
    }

    @Test
    void rotatesHttpOnlyRefreshCookieWithoutReturningItToJavaScript() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/browser/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Frontend Candidate",
                                  "email":"frontend-cookie-test@example.test",
                                  "password":"StrongPassword123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String registrationCookie = registration.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(registrationCookie)
                .contains(BrowserRefreshCookieService.COOKIE_NAME + "=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/v1/auth/browser");
        Cookie firstCookie = MockCookie.parse(registrationCookie);
        assertThat(firstCookie).isNotNull();

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/browser/refresh")
                        .cookie(firstCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();
        Cookie rotatedCookie = MockCookie.parse(
                refreshed.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(firstCookie.getValue());

        mockMvc.perform(post("/api/v1/auth/browser/logout").cookie(rotatedCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
        mockMvc.perform(post("/api/v1/auth/browser/refresh").cookie(rotatedCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidBrowserRegistrationWithoutIssuingACookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/browser/refresh"))
                .andExpect(status().isUnauthorized());

        String response = mockMvc.perform(post("/api/v1/auth/browser/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn().getResponse().getContentAsString();

        JsonNode error = objectMapper.readTree(response);
        assertThat(error.path("fieldErrors").has("email")).isTrue();
        assertThat(error.path("fieldErrors").has("password")).isTrue();
    }
}
