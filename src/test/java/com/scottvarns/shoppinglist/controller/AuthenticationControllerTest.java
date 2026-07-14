package com.scottvarns.shoppinglist.controller;

import com.scottvarns.shoppinglist.dto.request.LoginRequestDTO;
import com.scottvarns.shoppinglist.dto.request.SignupRequestDTO;
import com.scottvarns.shoppinglist.dto.response.AuthenticationResponseDTO;
import com.scottvarns.shoppinglist.dto.response.UserResponseDTO;
import com.scottvarns.shoppinglist.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.MethodName.class)
@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setupMockMvc() {
        // Configure MockMvc with the authentication controller under test
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
    }

    /**
     * When valid signup details are submitted
     * Then return the created user's details from the authentication service.
     */
    @Test
    void test01_signup_whenValidRequestProvided_thenReturnsCreatedUser() {
        // Create a valid signup request and expected response
        SignupRequestDTO request = new SignupRequestDTO(
                "user@example.com",
                "your-password",
                "Your Name"
        );

        UserResponseDTO createdUser = new UserResponseDTO(
                1L,
                "user@example.com",
                "Your Name"
        );

        // Mock the service layer to return the expected user
        when(authenticationService.signup(request)).thenReturn(createdUser);

        // Invoke the controller endpoint
        ResponseEntity<UserResponseDTO> response = authenticationController.signup(UUID.randomUUID(), request);

        // Assert the response and verify that the service layer received the request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(createdUser);
        verify(authenticationService).signup(request);
    }

    /**
     * When valid login credentials are submitted
     * Then return the JWT authentication response from the authentication service.
     */
    @Test
    void test02_login_whenValidRequestProvided_thenReturnsAuthenticationResponse() {
        // Create a valid login request and expected authentication response
        LoginRequestDTO request = new LoginRequestDTO(
                "user@example.com",
                "your-password"
        );

        AuthenticationResponseDTO authenticationResponse = new AuthenticationResponseDTO("jwt-token");

        // Mock the service layer to return the expected JWT response
        when(authenticationService.login(request)).thenReturn(authenticationResponse);

        // Invoke the controller endpoint
        ResponseEntity<AuthenticationResponseDTO> response =
                authenticationController.login(UUID.randomUUID(), request);

        // Assert the response and verify that the service layer received the request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(authenticationResponse);
        verify(authenticationService).login(request);
    }

    /**
     * When signup is requested without an X-Correlation-ID header
     * Then reject the request with a 400 Bad Request response.
     */
    @Test
    void test03_signup_whenCorrelationIdIsMissing_thenReturns400() throws Exception {
        // Invoke the signup endpoint without the X-Correlation-ID header and assert that a 400 response is returned
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\",\"password\":\"your-password\",\"name\":\"Your Name\"}"))
                .andExpect(status().isBadRequest());

        // Verify that validation rejected the request before the service layer was called
        verifyNoInteractions(authenticationService);
    }

    /**
     * When login is requested without an X-Correlation-ID header
     * Then reject the request with a 400 Bad Request response.
     */
    @Test
    void test04_login_whenCorrelationIdIsMissing_thenReturns400() throws Exception {
        // Invoke the login endpoint without the X-Correlation-ID header and assert that a 400 response is returned
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\",\"password\":\"your-password\"}"))
                .andExpect(status().isBadRequest());

        // Verify that validation rejected the request before the service layer was called
        verifyNoInteractions(authenticationService);
    }

}
