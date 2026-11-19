package com.lifesync.lifesync.auth.service.impl;

import com.lifesync.lifesync.auth.dto.*;
import com.lifesync.lifesync.auth.entity.RefreshToken;
import com.lifesync.lifesync.auth.repository.RefreshTokenRepository;
import com.lifesync.lifesync.auth.service.AuthService;
import com.lifesync.lifesync.auth.util.RefreshTokenGenerator;
import com.lifesync.lifesync.exception.UnauthorizedException;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import com.lifesync.lifesync.security.service.JwtService;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String INVALID_REFRESH = "Refresh token is invalid or expired";
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final long refreshExpirationMillis;

    public AuthServiceImpl(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder, JwtService jwtService,
                           RefreshTokenGenerator refreshTokenGenerator,
                           @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshExpirationMillis) {
        this.userRepository = userRepository; this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder; this.jwtService = jwtService;
        this.refreshTokenGenerator = refreshTokenGenerator; this.refreshExpirationMillis = refreshExpirationMillis;
        if (refreshExpirationMillis <= 0) throw new IllegalStateException("app.jwt.refresh-token-expiration must be positive");
    }

    @Override @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || !passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        String rawRefresh = createRefreshToken(user);
        String access = jwtService.generateAccessToken(new CustomUserDetails(user));
        return new LoginResponse("Login successful", access, rawRefresh, "Bearer", jwtService.getExpirationSeconds());
    }

    @Override @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse refresh(RefreshTokenRequest request) {
        String hash = refreshTokenGenerator.hash(request.getRefreshToken());
        RefreshToken old = refreshTokenRepository.findByTokenHashForUpdate(hash).orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH));
        if (old.isRevoked()) {
            // A reused rotated token indicates replay; invalidate all currently active refresh sessions for this account.
            refreshTokenRepository.findAllByUser_IdAndRevokedFalse(old.getUser().getId()).forEach(token -> token.setRevoked(true));
            throw new UnauthorizedException(INVALID_REFRESH);
        }
        if (!old.getExpiryDate().isAfter(Instant.now())) { old.setRevoked(true); throw new UnauthorizedException(INVALID_REFRESH); }
        User user = old.getUser();
        if (!Boolean.TRUE.equals(user.getActive())) { old.setRevoked(true); throw new UnauthorizedException(INVALID_REFRESH); }
        old.setRevoked(true);
        String rawRefresh = createRefreshToken(user);
        String access = jwtService.generateAccessToken(new CustomUserDetails(user));
        return new TokenResponse(access, rawRefresh, "Bearer", jwtService.getExpirationSeconds());
    }

    @Override @Transactional
    public void logout(RefreshTokenRequest request) {
        String hash = refreshTokenGenerator.hash(request.getRefreshToken());
        refreshTokenRepository.findByTokenHashForUpdate(hash).ifPresent(token -> token.setRevoked(true));
    }

    private String createRefreshToken(User user) {
        String raw = refreshTokenGenerator.generate();
        RefreshToken token = new RefreshToken(); token.setUser(user);
        token.setTokenHash(refreshTokenGenerator.hash(raw));
        token.setExpiryDate(Instant.now().plusMillis(refreshExpirationMillis));
        refreshTokenRepository.save(token);
        return raw;
    }
}
