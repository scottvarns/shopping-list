package com.scottvarns.shoppinglist.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CorrelationIdInterceptor implements HandlerInterceptor {

    static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Add the request correlation ID to the logging context for controller and service logs
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        } else {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        }

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        // Clear the logging context to prevent the correlation ID leaking into another request on the same thread
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }
}
