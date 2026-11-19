package com.lifesync.lifesync.user.service;

import com.lifesync.lifesync.user.dto.ChangePasswordRequest;
import com.lifesync.lifesync.user.dto.UpdateUserRequest;
import com.lifesync.lifesync.user.dto.UserRequest;
import com.lifesync.lifesync.user.dto.UserResponse;

public interface UserService {
    UserResponse registerUser(UserRequest request);
    UserResponse getMyProfile(String email);
    UserResponse updateMyProfile(String email, UpdateUserRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    void deactivateAccount(String email);
}
