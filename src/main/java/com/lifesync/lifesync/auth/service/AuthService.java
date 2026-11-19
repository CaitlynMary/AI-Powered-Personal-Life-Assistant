package com.lifesync.lifesync.auth.service;
import com.lifesync.lifesync.auth.dto.LoginRequest;
import com.lifesync.lifesync.auth.dto.LoginResponse;
import com.lifesync.lifesync.auth.dto.RefreshTokenRequest;
import com.lifesync.lifesync.auth.dto.TokenResponse;
public interface AuthService {
    LoginResponse login(LoginRequest request);
    TokenResponse refresh(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}
