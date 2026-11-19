package com.lifesync.lifesync.security.config;

import com.lifesync.lifesync.common.ApiConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    public RestAccessDeniedHandler() { }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":403,\"error\":\"Forbidden\",\"message\":\""
                + ApiConstants.ACCESS_DENIED + "\",\"validationErrors\":null}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
