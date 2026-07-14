package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.LoginRequestDTO;
import com.scottvarns.shoppinglist.dto.request.SignupRequestDTO;
import com.scottvarns.shoppinglist.dto.response.AuthenticationResponseDTO;
import com.scottvarns.shoppinglist.dto.response.UserResponseDTO;
import com.scottvarns.shoppinglist.entity.User;
import com.scottvarns.shoppinglist.exception.ConflictException;
import com.scottvarns.shoppinglist.exception.UnauthorizedException;
import com.scottvarns.shoppinglist.repository.UserRepository;
import com.scottvarns.shoppinglist.security.JwtService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.MethodName.class)
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    /**
     * When valid signup details are provided for an unused email address, then create
     * the user with an encoded password and return the created user's safe details.
     */
    @Test
    void test01_signup_whenEmailIsUnused_thenCreatesUserWithEncodedPassword() {
        // Create a SignupRequestDTO with valid details and a User entity representing the saved user
        SignupRequestDTO request = new SignupRequestDTO(
                "user@example.com",
                "your-password",
                "Your Name"
        );
        // Create a User entity representing the saved user with an encoded password
        User savedUser = User.builder()
                .userId(1L)
                .email(request.email())
                .passwordHash("encoded-password")
                .name(request.name())
                .build();

        // Mock the behavior of the userRepository and passwordEncoder to simulate the signup process
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Call the signup method and capture the response
        UserResponseDTO response = authenticationService.signup(request);

        // Assert that the response matches the expected UserResponseDTO
        assertThat(response).isEqualTo(new UserResponseDTO(1L, "user@example.com", "Your Name"));

        // Verify that the userRepository.save method was called with the correct User entity and assert that the password hash is as expected
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    /**
     * When signup details use an existing email address,
     * Then reject the request without encoding or persisting a password and throw a ConflictException with the expected message.
     */
    @Test
    void test02_signup_whenEmailAlreadyExists_thenThrowsConflictException() {
        // Create a SignupRequestDTO with an email that already exists in the system
        SignupRequestDTO request = new SignupRequestDTO(
                "user@example.com",
                "your-password",
                "Your Name"
        );

        // Mock the behavior of the userRepository to simulate that the email already exists
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // Call the signup method and assert that it throws a ConflictException with the expected message
        assertThatThrownBy(() -> authenticationService.signup(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An account already exists for this email address.");

        // Verify that the passwordEncoder.encode and userRepository.save methods are not called
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    /**
     * When valid credentials are provided
     * Then return the JWT created for that user
     */
    @Test
    void test03_login_whenCredentialsAreValid_thenReturnsJwt() {
        // Create a LoginRequestDTO with valid credentials and a User entity representing the user in the system
        LoginRequestDTO request = new LoginRequestDTO(
                "user@example.com",
                "your-password"
        );

        User user = User.builder()
                .userId(1L)
                .email(request.email())
                .passwordHash("encoded-password")
                .build();

        // Mock the behavior of the userRepository, passwordEncoder, and jwtService to simulate a successful login process
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(1L, request.email())).thenReturn("jwt-token");

        // Call the login method and capture the response
        AuthenticationResponseDTO response = authenticationService.login(request);

        // Assert that the response contains the expected JWT token
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    /**
     * When the supplied password does not match the stored hash
     * Then reject login and throw an UnauthorizedException with the expected message
     */
    @Test
    void test04_login_whenPasswordIsInvalid_thenThrowsUnauthorizedException() {
        // Create a LoginRequestDTO with an invalid password and a User entity representing the user in the system
        LoginRequestDTO request = new LoginRequestDTO(
                "user@example.com",
                "wrong-password"
        );

        User user = User.builder()
                .email(request.email())
                .passwordHash("encoded-password")
                .build();

        // Mock the behavior of the userRepository and passwordEncoder to simulate an invalid password scenario
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        // Call the login method and assert that it throws an UnauthorizedException with the expected message
        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password.");
    }

    /**
     * When no user exists for the supplied email address
     * Then reject login without checking a password or generating a JWT.
     */
    @Test
    void test05_login_whenUserDoesNotExist_thenThrowsUnauthorizedException() {
        // Create a LoginRequestDTO with an email that does not exist in the system
        LoginRequestDTO request = new LoginRequestDTO(
                "unknown@example.com",
                "your-password"
        );

        // Mock the behavior of the userRepository to simulate that no user exists for the provided email
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // Call the login method and assert that it throws an UnauthorizedException with the expected message
        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password.");

        // Verify that the passwordEncoder and jwtService are not interacted with since the user does not exist
        verifyNoInteractions(passwordEncoder, jwtService);
    }
}
