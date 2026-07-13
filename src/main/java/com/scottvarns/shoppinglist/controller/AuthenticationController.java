package com.scottvarns.shoppinglist.controller;

import com.scottvarns.shoppinglist.dto.request.LoginRequestDTO;
import com.scottvarns.shoppinglist.dto.request.SignupRequestDTO;
import com.scottvarns.shoppinglist.dto.response.AuthenticationResponseDTO;
import com.scottvarns.shoppinglist.dto.response.UserResponseDTO;
import com.scottvarns.shoppinglist.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationService authenticationService;

    /**
     * Endpoint to handle user signup requests.
     * @param correlationId     Correlation ID for end-to-end traceability
     * @param request           The signup request payload containing user details
     *
     * @return ResponseEntity containing details of the newly created user
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDTO> signup(
            @RequestHeader(value = "X-Correlation-ID") UUID correlationId,
            @Valid @RequestBody SignupRequestDTO request
    ) {
        log.info("Received signup request");
        return ResponseEntity.status(201).body(authenticationService.signup(request));
    }

    /**
     * Endpoint to handle user login requests.
     * @param correlationId correlation ID for end-to-end traceability
     * @param request the login request payload containing user credentials
     *
     * @return ResponseEntity containing authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> login(
            @RequestHeader("X-Correlation-ID") UUID correlationId,
            @Valid @RequestBody LoginRequestDTO request
    ) {
        log.info("Received login request");
        return ResponseEntity.ok(authenticationService.login(request));
    }
}
