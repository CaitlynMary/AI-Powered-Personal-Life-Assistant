package com.lifesync.lifesync.ai.service;

import com.openai.errors.OpenAIServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class AiFailureDiagnostics {
    private AiFailureDiagnostics() { }

    public static String rootCauseType(Throwable failure) {
        return rootCause(failure).getClass().getSimpleName();
    }

    public static int httpStatus(Throwable failure) {
        for (Throwable current = failure; current != null && current.getCause() != current; current = current.getCause()) {
            if (current instanceof OpenAIServiceException serviceException) return serviceException.statusCode();
            if (current instanceof RestClientResponseException responseException) {
                return responseException.getStatusCode().value();
            }
        }
        return -1;
    }

    public static String retryAfter(Throwable failure) {
        for (Throwable current = failure; current != null && current.getCause() != current; current = current.getCause()) {
            if (current instanceof OpenAIServiceException serviceException) {
                String value = serviceException.headers().values(HttpHeaders.RETRY_AFTER).stream().findFirst().orElse(null);
                String validatedValue = validRetryAfter(value);
                if (validatedValue != null) return validatedValue;
            }
            if (current instanceof RestClientResponseException responseException) {
                HttpHeaders headers = responseException.getResponseHeaders();
                String validatedValue = validRetryAfter(headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER));
                if (validatedValue != null) return validatedValue;
            }
        }
        return null;
    }

    public static String safeMessage(Throwable failure) {
        Throwable rootCause = rootCause(failure);
        String message = rootCause.getMessage();
        if (message == null || message.isBlank()) message = failure.getMessage();
        if (message == null || message.isBlank()) return "not provided";
        return AiContentSanitizer.safeText(message, 240);
    }

    private static String validRetryAfter(String value) {
        if (value == null) return null;
        String candidate = value.trim();
        if (candidate.matches("\\d{1,10}")) return candidate;
        try {
            ZonedDateTime.parse(candidate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return candidate;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current;
    }
}
