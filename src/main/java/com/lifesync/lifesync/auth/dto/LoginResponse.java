package com.lifesync.lifesync.auth.dto;
public class LoginResponse extends TokenResponse {
    private String message;
    public LoginResponse() { }
    public LoginResponse(String message, String accessToken, String refreshToken, String tokenType, long expiresIn) {
        super(accessToken, refreshToken, tokenType, expiresIn); this.message = message;
    }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
