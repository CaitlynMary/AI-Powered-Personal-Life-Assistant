package com.lifesync.lifesync.exception;

import com.lifesync.lifesync.common.ApiConstants;
import com.lifesync.lifesync.ai.exception.AiContextException;
import com.lifesync.lifesync.ai.exception.AiProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsAccessDeniedToForbidden() {
        var response = handler.accessDenied(new AccessDeniedException("internal detail"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ApiConstants.ACCESS_DENIED, response.getBody().getMessage());
    }

    @Test
    void unexpectedErrorsDoNotExposeInternalDetails() {
        var response = handler.internal();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ApiConstants.UNEXPECTED_ERROR, response.getBody().getMessage());
        assertNull(response.getBody().getValidationErrors());
    }

    @Test
    void mapsResourceNotFoundTo404() {
        var response = handler.notFound(new ResourceNotFoundException("Missing resource"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Missing resource", response.getBody().getMessage());
    }

    @Test
    void mapsMissingQueryParametersToBadRequest() {
        var response = handler.invalidRequestParameter(new MissingServletRequestParameterException("date", "LocalDate"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void mapsAiConfigurationAndQuotaFailuresToActionableStatuses() {
        var configuration = handler.aiProviderUnavailable(
                new AiProviderException(AiProviderException.FailureType.CONFIGURATION));
        var quota = handler.aiProviderUnavailable(
                new AiProviderException(AiProviderException.FailureType.RATE_LIMIT));
        var context = handler.aiContextUnavailable(new AiContextException());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, configuration.getStatusCode());
        assertTrue(configuration.getBody().getMessage().contains("AI provider configuration"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, quota.getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, context.getStatusCode());
    }

    @Test
    void mapsRateLimitRetryAfterHeaderAndTimeoutToGatewayTimeout() {
        var quota = handler.aiProviderUnavailable(
                new AiProviderException(AiProviderException.FailureType.RATE_LIMIT, "45"));
        var response = handler.aiProviderUnavailable(
                new AiProviderException(AiProviderException.FailureType.TIMEOUT));

        assertEquals("45", quota.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals("The AI request timed out. Please retry.", response.getBody().getMessage());
    }
}
