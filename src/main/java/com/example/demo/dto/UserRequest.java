package com.example.demo.dto;

import com.example.demo.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creación de usuarios con rol explícito (solo ADMIN). */
public record UserRequest(
        @NotBlank(message = "username es obligatorio") String username,
        @NotBlank(message = "email es obligatorio") @Email(message = "email inválido") String email,
        @NotBlank(message = "password es obligatorio")
        @Size(min = 8, message = "password debe tener al menos 8 caracteres") String password,
        @NotNull(message = "role es obligatorio") Role role) {
}
