package com.lifesync.lifesync.ai.exception;

public class AiProviderException extends RuntimeException {
    public enum FailureType {
        CONFIGURATION,
        RATE_LIMIT,
        TIMEOUT,
        CONNECTION,
        UNAVAILABLE
    }

    private final FailureType failureType;
    private final String retryAfter;

    public AiProviderException() {
        this(FailureType.UNAVAILABLE);
    }

    public AiProviderException(FailureType failureType) {
        this(failureType, null);
    }

    public AiProviderException(FailureType failureType, String retryAfter) {
        super(safeMessage(failureType));
        this.failureType = failureType;
        this.retryAfter = retryAfter;
    }

    public FailureType getFailureType() { return failureType; }
    public String getRetryAfter() { return retryAfter; }

    private static String safeMessage(FailureType failureType) {
        return switch (failureType) {
            case CONFIGURATION -> "AI provider configuration is missing or invalid. Check the backend API key and account access.";
            case RATE_LIMIT -> "The AI provider rate limit has been reached. Please try again later.";
            case TIMEOUT -> "The AI request timed out. Please retry.";
            case CONNECTION -> "Unable to connect to the AI service. Check the backend network connection.";
            case UNAVAILABLE -> "The AI service is temporarily unavailable. Please try again.";
        };
    }
}
