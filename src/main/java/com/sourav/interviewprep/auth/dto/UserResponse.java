package com.sourav.interviewprep.auth.dto;

import com.sourav.interviewprep.auth.entity.UserEntity;

import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        boolean emailVerified,
        List<String> roles
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isEmailVerified(),
                user.getRoles().stream().map(role -> role.getName()).sorted().toList()
        );
    }
}
