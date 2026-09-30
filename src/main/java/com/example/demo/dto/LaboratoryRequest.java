package com.example.demo.dto;

import com.example.demo.entity.LabStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LaboratoryRequest(
        @NotBlank(message = "username es obligatorio") String username,
        @NotBlank(message = "email es obligatorio") @Email(message = "email inválido") String email,
        @NotBlank(message = "location es obligatorio") String location,
        @NotNull(message = "managerId es obligatorio") Long managerId,
        LabStatus status) {
}
