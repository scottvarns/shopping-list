package com.scottvarns.shoppinglist.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.MethodName.class)
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET =
            "c2hvcHBpbmdsaXN0LXRlc3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * When a request contains a valid bearer token
     * Then the filter authenticates the request with the token's user ID as its principal.
     */
    @Test
    void test01_doFilterInternal_whenValidBearerTokenProvided_thenSetsAuthenticatedUserId() throws Exception {
        JwtService jwtService = new JwtService(TEST_SECRET, 3600);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(42L, "user@example.com"));

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(42L)
        );
    }

    /**
     * When a request contains an invalid bearer token
     * Then the filter does not authenticate the request and clears the security context.
     */
    @Test
    void test02_doFilterInternal_whenInvalidBearerTokenProvided_thenClearsSecurityContext() throws Exception {
        JwtService jwtService = new JwtService(TEST_SECRET, 3600);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.here");

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull()
        );
    }

    /**
     * When a request contains an incorrect autnhorization header (not starting with "Bearer ")
     * Then the filter does not authenticate the request and clears the security context.
     */
    @Test
    void test03_doFilterInternal_whenInvalidAuthTypeProvided_thenClearsSecurityContext() throws Exception {
        JwtService jwtService = new JwtService(TEST_SECRET, 3600);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic 123");

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull()
        );
    }
}
