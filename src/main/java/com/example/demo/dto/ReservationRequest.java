package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservationRequest(
        @NotNull(message = "slotId es obligatorio") Long slotId,
        @NotBlank(message = "purpose es obligatorio")
        @Size(max = 250, message = "purpose no puede superar 250 caracteres") String purpose) {
}
