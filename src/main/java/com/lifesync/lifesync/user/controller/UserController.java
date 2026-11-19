package com.lifesync.lifesync.user.controller;

import com.lifesync.lifesync.exception.UnauthorizedException;
import com.lifesync.lifesync.user.dto.ChangePasswordRequest;
import com.lifesync.lifesync.user.dto.UpdateUserRequest;
import com.lifesync.lifesync.user.dto.UserRequest;
import com.lifesync.lifesync.user.dto.UserResponse;
import com.lifesync.lifesync.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) { this.userService = userService; }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    @GetMapping("/me")
    public UserResponse getMyProfile(Authentication authentication) {
        return userService.getMyProfile(authenticatedEmail(authentication));
    }

    @PutMapping("/me")
    public UserResponse updateMyProfile(Authentication authentication, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateMyProfile(authenticatedEmail(authentication), request);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authenticatedEmail(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/deactivate")
    public ResponseEntity<Void> deactivateAccount(Authentication authentication) {
        userService.deactivateAccount(authenticatedEmail(authentication));
        return ResponseEntity.noContent().build();
    }

    private String authenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Authentication is required for this operation");
        }
        return authentication.getName();
    }
}
