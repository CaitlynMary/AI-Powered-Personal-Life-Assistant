package com.lifesync.lifesync.ai.service;

import java.util.regex.Pattern;

public final class AiContentSanitizer {
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)\\b(password|passwd|api[ _-]?key|access[_-]?token|refresh[_-]?token|secret)\\s*(?:[:=]|\\bis\\b)\\s*[^\\s,;]+");
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+=*");
    private static final Pattern JWT_TOKEN = Pattern.compile(
            "\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b");
    private static final Pattern PROVIDER_API_KEY = Pattern.compile(
            "\\b(?:AIza[0-9A-Za-z_-]{20,}|gsk_[0-9A-Za-z_-]{20,}|sk-[0-9A-Za-z_-]{20,})\\b");

    private AiContentSanitizer() { }

    public static String redact(String value) {
        String sanitized = BEARER_TOKEN.matcher(value).replaceAll("[REDACTED_CREDENTIAL]");
        sanitized = JWT_TOKEN.matcher(sanitized).replaceAll("[REDACTED_CREDENTIAL]");
        sanitized = PROVIDER_API_KEY.matcher(sanitized).replaceAll("[REDACTED_CREDENTIAL]");
        return SENSITIVE_ASSIGNMENT.matcher(sanitized).replaceAll("$1=[REDACTED]");
    }

    public static String safeText(String value, int maxLength) {
        if (value == null) return "";
        String sanitized = redact(value.replace('\n', ' ').replace('\r', ' ').trim());
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
