package com.lifesync.lifesync.user.service;

import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.user.dto.ChangePasswordRequest;
import com.lifesync.lifesync.user.dto.UpdateUserRequest;
import com.lifesync.lifesync.user.dto.UserRequest;
import com.lifesync.lifesync.user.dto.UserResponse;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    private UserServiceImpl service;

    @BeforeEach void setUp() { service = new UserServiceImpl(userRepository, passwordEncoder); }

    @Test void registersWithHashedPasswordAndPasswordFreeResponse() {
        when(userRepository.existsByEmail("john@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0); user.setId(5L); return user;
        });
        UserResponse response = service.registerUser(new UserRequest("John Smith", "John@gmail.com", "Password123", "9876543210"));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("bcrypt-hash", captor.getValue().getPassword());
        assertTrue(captor.getValue().getActive());
        assertEquals("john@gmail.com", response.getEmail());
        assertFalse(java.util.Arrays.stream(UserResponse.class.getDeclaredFields()).anyMatch(f -> f.getName().toLowerCase().contains("password")));
        verify(passwordEncoder).encode("Password123");
    }

    @Test void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("john@gmail.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.registerUser(new UserRequest("John", "john@gmail.com", "Password123", null)));
        verify(userRepository, never()).save(any());
    }

    @Test void validatesRegistrationEmailAndRequiredFields() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        UserRequest invalid = new UserRequest(" ", "not-an-email", "short", null);
        var violations = validator.validate(invalid);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test void retrievesAndUpdatesOnlyOwnProfileFields() {
        User user = user();
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals(1L, service.getMyProfile("john@gmail.com").getId());
        UserResponse updated = service.updateMyProfile("john@gmail.com", new UpdateUserRequest("Jane Doe", "+919876543210"));
        assertEquals("Jane Doe", updated.getFullName());
        assertEquals("john@gmail.com", updated.getEmail());
        assertEquals("+919876543210", updated.getPhoneNumber());
        assertNotNull(user.getUpdatedAt());
    }

    @Test void rejectsIncorrectCurrentPassword() {
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.changePassword("john@gmail.com", new ChangePasswordRequest("wrong", "NewPassword123", "NewPassword123")));
        verify(userRepository, never()).save(any());
    }

    @Test void rejectsPasswordConfirmationMismatch() {
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.changePassword("john@gmail.com", new ChangePasswordRequest("old", "NewPassword123", "OtherPassword123")));
        verify(userRepository, never()).save(any());
    }

    @Test void changesPasswordAfterValidationAndHashing() {
        User user = user();
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hash");
        service.changePassword("john@gmail.com", new ChangePasswordRequest("old", "NewPassword123", "NewPassword123"));
        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test void deactivatesWithoutDeleting() {
        User user = user();
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        service.deactivateAccount("john@gmail.com");
        assertFalse(user.getActive());
        verify(userRepository, never()).delete(any());
    }

    private User user() {
        User user = new User(); user.setId(1L); user.setFullName("John Smith"); user.setEmail("john@gmail.com");
        user.setPassword("old-hash"); user.setActive(true); return user;
    }
}
