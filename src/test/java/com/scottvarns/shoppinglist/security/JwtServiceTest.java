package com.scottvarns.shoppinglist.security;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.MethodName.class)
class JwtServiceTest {

    private static final String TEST_SECRET =
            "c2hvcHBpbmdsaXN0LXRlc3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz";

    /**
     * When a token is generated for an authenticated user
     * Then its user ID can be extracted after the token is verified.
     */
    @Test
    void test01_extractUserId_whenValidTokenProvided_thenReturnsAuthenticatedUserId() {
        // Create a JwtService with a test secret and a token expiration time of 3600 seconds (1 hour)
        JwtService jwtService = new JwtService(TEST_SECRET, 3600);

        // Generate a token for user ID 42 and email
        String token = jwtService.generateToken(42L, "user@example.com");

        // Extract the user ID from the token and assert that it is equal to 42L
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }
}
