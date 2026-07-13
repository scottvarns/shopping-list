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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Creates a new user account using the supplied signup details.
     *
     * @param request the user's email address, password, and display name
     * @return safe details of the newly created user
     */
    @Override
    public UserResponseDTO signup(SignupRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Rejected signup request because the email address is already registered");
            throw new ConflictException("An account already exists for this email address.");
        }

        User savedUser = userRepository.save(User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build());

        log.info("Created user account [userId={}]", savedUser.getUserId());
        return new UserResponseDTO(savedUser.getUserId(), savedUser.getEmail(), savedUser.getName());
    }

    /**
     * Authenticates a user and creates a JWT bearer token.
     *
     * @param request the user's email address and password
     * @return an authentication response containing the JWT
     */
    @Override
    public AuthenticationResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            log.warn("Rejected login request because the credentials are invalid");
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Rejected login request because the credentials are invalid");
            throw new UnauthorizedException("Invalid email or password.");
        }

        log.info("Authenticated user [userId={}]", user.getUserId());
        return new AuthenticationResponseDTO(jwtService.generateToken(user.getUserId(), user.getEmail()));
    }
}
