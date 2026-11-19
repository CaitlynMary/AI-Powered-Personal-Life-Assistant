package com.lifesync.lifesync.user.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.user.dto.ChangePasswordRequest;
import com.lifesync.lifesync.user.dto.UpdateUserRequest;
import com.lifesync.lifesync.user.dto.UserRequest;
import com.lifesync.lifesync.user.dto.UserResponse;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse registerUser(UserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(blankToNull(request.getPhoneNumber()));
        user.setActive(true);
        try {
            return toResponse(userRepository.save(user));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) { return toResponse(findByEmail(email)); }

    @Override
    @Transactional
    public UserResponse updateMyProfile(String email, UpdateUserRequest request) {
        User user = findByEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(blankToNull(request.getPhoneNumber()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateAccount(String email) {
        User user = findByEmail(email);
        user.setActive(false);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    private User findByEmail(String email) {
        if (email == null || email.isBlank()) throw new BadRequestException("Authenticated user email is unavailable");
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
        if (!Boolean.TRUE.equals(user.getActive())) throw new BadRequestException("Account is inactive");
        return user;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhoneNumber(),
                user.getActive(), user.getCreatedAt());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
