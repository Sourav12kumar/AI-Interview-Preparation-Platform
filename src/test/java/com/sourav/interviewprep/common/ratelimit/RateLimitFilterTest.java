package com.sourav.interviewprep.common.ratelimit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void rejectsRequestsBeyondTheConfiguredWindowLimit() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimitFilter filter = filter(registry);

        MockHttpServletResponse first = invoke(filter, "/api/v1/auth/login");
        MockHttpServletResponse second = invoke(filter, "/api/v1/auth/login");
        MockHttpServletResponse third = invoke(filter, "/api/v1/auth/login");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isNotBlank();
        assertThat(third.getContentAsString()).contains("Request rate limit exceeded");
        assertThat(registry.counter("rate_limit_rejections_total", "scope", "auth").count())
                .isEqualTo(1.0);
    }

    @Test
    void excludesInfrastructureHealthEndpoints() throws Exception {
        RateLimitFilter filter = filter(new SimpleMeterRegistry());

        for (int count = 0; count < 5; count++) {
            assertThat(invoke(filter, "/actuator/health").getStatus()).isEqualTo(200);
        }
    }

    private RateLimitFilter filter(SimpleMeterRegistry registry) {
        RateLimitProperties properties = new RateLimitProperties(
                true, Duration.ofMinutes(1), 2, 2, 2, 100);
        return new RateLimitFilter(properties, registry, new ObjectMapper(), Clock.systemUTC());
    }

    private MockHttpServletResponse invoke(RateLimitFilter filter, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("192.0.2.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
