package com.rentvideo.dto;

import com.rentvideo.entity.Role;
import com.rentvideo.entity.User;

public record UserResponse(Long id, String email, String firstName, String lastName, Role role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole());
    }
}
