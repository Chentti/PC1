package com.example.demo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username es obligatorio") String username,
        @NotBlank(message = "email es obligatorio") @Email(message = "email inválido") String email,
        @NotBlank(message = "password es obligatorio")
        @Size(min = 8, message = "password debe tener al menos 8 caracteres") String password) {
}
