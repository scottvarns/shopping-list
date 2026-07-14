package com.scottvarns.shoppinglist.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.MethodName.class)
class CorrelationIdInterceptorTest {

    private final CorrelationIdInterceptor correlationIdInterceptor = new CorrelationIdInterceptor();

    @AfterEach
    void clearLoggingContext() {
        MDC.clear();
    }

    /**
     * When a request contains an X-Correlation-ID header
     * Then the interceptor adds the correlation ID to the logging context for the duration of the request.
     */
    @Test
    void test01_preHandle_whenCorrelationIdProvided_thenAddsCorrelationIdToLoggingContext() throws Exception {
        // Create a request containing a correlation ID header
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdInterceptor.CORRELATION_ID_HEADER,
                "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3");

        // Invoke the interceptor and assert that request processing continues with the correlation ID in the MDC
        boolean shouldContinue = correlationIdInterceptor.preHandle(
                request, new MockHttpServletResponse(), new Object());

        assertThat(shouldContinue).isTrue();
        assertThat(MDC.get(CorrelationIdInterceptor.CORRELATION_ID_MDC_KEY))
                .isEqualTo("e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3");
    }

    /**
     * When request processing completes
     * Then the interceptor removes the correlation ID from the logging context.
     */
    @Test
    void test02_afterCompletion_whenCorrelationIdIsInLoggingContext_thenClearsCorrelationId() throws Exception {
        // Add a correlation ID to the MDC to represent an active request
        MDC.put(CorrelationIdInterceptor.CORRELATION_ID_MDC_KEY,
                "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3");

        // Complete request processing and assert that the correlation ID no longer remains in the MDC
        correlationIdInterceptor.afterCompletion(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object(), null);

        assertThat(MDC.get(CorrelationIdInterceptor.CORRELATION_ID_MDC_KEY)).isNull();
    }

    /**
     * When a request does not contain an X-Correlation-ID header
     * Then the interceptor removes any stale correlation ID and leaves validation to the controller.
     */
    @Test
    void test03_preHandle_whenCorrelationIdIsMissing_thenClearsStaleCorrelationId() throws Exception {
        // Add a stale correlation ID to the MDC to represent a previously handled request
        MDC.put(CorrelationIdInterceptor.CORRELATION_ID_MDC_KEY,
                "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3");

        // Invoke the interceptor without a correlation ID header and assert that no stale value is retained
        boolean shouldContinue = correlationIdInterceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertThat(shouldContinue).isTrue();
        assertThat(MDC.get(CorrelationIdInterceptor.CORRELATION_ID_MDC_KEY)).isNull();
    }
}
