package com.scottvarns.shoppinglist.exception;

import com.scottvarns.shoppinglist.dto.response.ErrorResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.MethodName.class)
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

    /**
     * When a BadRequestException is handled
     * Then return a 400 Bad Request response containing the exception message.
     */
    @Test
    void test01_handleValidationException_whenBadRequestExceptionThrown_thenReturns400() {
        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleValidationException(new BadRequestException("Invalid shopping list"));

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid shopping list");
    }

    /**
     * When an UnauthorizedException is handled
     * Then return a 401 Unauthorized response containing the exception message.
     */
    @Test
    void test02_handleUnauthorizedException_whenUnauthorizedExceptionThrown_thenReturns401() {
        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleUnauthorizedException(new UnauthorizedException("Authentication is required"));

        assertErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    /**
     * When a ForbiddenException is handled
     * Then return a 403 Forbidden response containing the exception message.
     */
    @Test
    void test03_handleForbiddenException_whenForbiddenExceptionThrown_thenReturns403() {
        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleForbiddenException(new ForbiddenException("Access denied"));

        assertErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    /**
     * When a NotFoundException is handled
     * Then return a 404 Not Found response containing the exception message.
     */
    @Test
    void test04_handleNotFoundException_whenNotFoundExceptionThrown_thenReturns404() {
        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleNotFoundException(new NotFoundException("Shopping list not found"));

        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Shopping list not found");
    }

    /**
     * When a ConflictException is handled
     * Then return a 409 Conflict response containing the exception message.
     */
    @Test
    void test05_handleConflictException_whenConflictExceptionThrown_thenReturns409() {
        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleConflictException(new ConflictException("Shopping list already exists"));

        assertErrorResponse(response, HttpStatus.CONFLICT, "Shopping list already exists");
    }

    private void assertErrorResponse(
            ResponseEntity<ErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedMessage
    ) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(body -> {
                    assertThat(body.error()).isEqualTo(expectedStatus.name());
                    assertThat(body.message()).isEqualTo(expectedMessage);
                });
    }
}
