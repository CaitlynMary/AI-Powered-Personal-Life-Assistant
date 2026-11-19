package com.lifesync.lifesync.auth.service;

import com.lifesync.lifesync.auth.dto.LoginRequest;
import com.lifesync.lifesync.auth.dto.RefreshTokenRequest;
import com.lifesync.lifesync.auth.entity.RefreshToken;
import com.lifesync.lifesync.auth.repository.RefreshTokenRepository;
import com.lifesync.lifesync.auth.service.impl.AuthServiceImpl;
import com.lifesync.lifesync.auth.util.RefreshTokenGenerator;
import com.lifesync.lifesync.exception.UnauthorizedException;
import com.lifesync.lifesync.security.service.JwtService;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository tokens;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    private RefreshTokenGenerator generator;
    private AuthServiceImpl service;
    private User user;

    @BeforeEach void setUp() {
        generator = new RefreshTokenGenerator();
        service = new AuthServiceImpl(users, tokens, encoder, jwt, generator, 604800000);
        user = new User(); user.setId(3L); user.setEmail("john@example.com"); user.setPassword("bcrypt"); user.setActive(true);
    }

    @Test void successfulLoginCreatesRefreshHashAndReturnsTokenPair() {
        when(users.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("Password123", "bcrypt")).thenReturn(true);
        when(jwt.generateAccessToken(any())).thenReturn("access"); when(jwt.getExpirationSeconds()).thenReturn(900L);
        var response = service.login(new LoginRequest("JOHN@example.com", "Password123"));
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).save(captor.capture());
        assertEquals("access", response.getAccessToken());
        assertNotEquals(response.getRefreshToken(), captor.getValue().getTokenHash());
        assertEquals(generator.hash(response.getRefreshToken()), captor.getValue().getTokenHash());
        assertFalse(response.getRefreshToken().isBlank());
    }

    @Test void rejectsIncorrectAndInactiveLogin() {
        when(users.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("bad", "bcrypt")).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("john@example.com", "bad")));
        user.setActive(false);
        assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("john@example.com", "Password123")));
        verify(tokens, never()).save(any());
    }

    @Test void rotatesRefreshTokenAndRevokesOldHash() {
        String rawOld = "existing-refresh";
        RefreshToken old = token(rawOld);
        when(tokens.findByTokenHashForUpdate(generator.hash(rawOld))).thenReturn(Optional.of(old));
        when(jwt.generateAccessToken(any())).thenReturn("new-access"); when(jwt.getExpirationSeconds()).thenReturn(900L);
        var response = service.refresh(new RefreshTokenRequest(rawOld));
        assertTrue(old.isRevoked());
        assertEquals("new-access", response.getAccessToken());
        assertNotEquals(rawOld, response.getRefreshToken());
        verify(tokens).save(argThat(saved -> saved.getTokenHash().equals(generator.hash(response.getRefreshToken()))));
    }

    @Test void rejectsReplayAndRevokesOtherSessions() {
        RefreshToken reused = token("reused"); reused.setRevoked(true);
        RefreshToken other = token("other");
        when(tokens.findByTokenHashForUpdate(generator.hash("reused"))).thenReturn(Optional.of(reused));
        when(tokens.findAllByUser_IdAndRevokedFalse(3L)).thenReturn(List.of(other));
        assertThrows(UnauthorizedException.class, () -> service.refresh(new RefreshTokenRequest("reused")));
        assertTrue(other.isRevoked());
    }

    @Test void logoutRevokesRefreshTokenAndIsIdempotentForUnknownToken() {
        RefreshToken token = token("logout");
        when(tokens.findByTokenHashForUpdate(generator.hash("logout"))).thenReturn(Optional.of(token));
        service.logout(new RefreshTokenRequest("logout"));
        assertTrue(token.isRevoked());
        when(tokens.findByTokenHashForUpdate(generator.hash("unknown"))).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.logout(new RefreshTokenRequest("unknown")));
    }

    private RefreshToken token(String raw) {
        RefreshToken token = new RefreshToken(); token.setUser(user); token.setTokenHash(generator.hash(raw));
        token.setExpiryDate(Instant.now().plusSeconds(3600)); return token;
    }
}
