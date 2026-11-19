package com.lifesync.lifesync.security.service;

import com.lifesync.lifesync.security.principal.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    @Value("${app.jwt.secret}") private String encodedSecret;
    @Value("${app.jwt.access-token-expiration:900000}") private long expirationMillis;
    private SecretKey signingKey;

    @PostConstruct
    void initializeKey() {
        try {
            byte[] key = Decoders.BASE64.decode(encodedSecret);
            if (key.length < 32) throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
            signingKey = Keys.hmacShaKeyFor(key);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid JWT_SECRET: provide a Base64-encoded secret of at least 32 bytes", exception);
        }
        if (expirationMillis <= 0) throw new IllegalStateException("app.jwt.access-token-expiration must be positive");
    }

    public String generateAccessToken(CustomUserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getUsername()).claim("uid", user.getId())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(signingKey).compact();
    }

    public String extractSubject(String token) { return parseClaims(token).getSubject(); }
    public boolean isTokenValid(String token, UserDetails user) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equalsIgnoreCase(user.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
    public long getExpirationSeconds() { return expirationMillis / 1000; }
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
