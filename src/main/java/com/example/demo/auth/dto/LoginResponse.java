package com.example.demo.auth.dto;

public record LoginResponse(String token, long expiresIn) {
}
