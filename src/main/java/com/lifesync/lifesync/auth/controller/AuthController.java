package com.lifesync.lifesync.auth.controller;
import com.lifesync.lifesync.auth.dto.LoginRequest;
import com.lifesync.lifesync.auth.dto.LoginResponse;
import com.lifesync.lifesync.auth.dto.RefreshTokenRequest;
import com.lifesync.lifesync.auth.dto.TokenResponse;
import com.lifesync.lifesync.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }
    @PostMapping("/refresh") public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) { return authService.refresh(request); }
    @PostMapping("/logout") public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request); return ResponseEntity.noContent().build();
    }
}
