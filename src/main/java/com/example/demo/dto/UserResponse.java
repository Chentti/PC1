package com.example.demo.dto;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;

public record UserResponse(Long id, String username, String email, Role role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
    }
}
