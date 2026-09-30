package com.example.demo.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record SlotRequest(
        @NotNull(message = "laboratoryId es obligatorio") Long laboratoryId,
        @NotBlank(message = "equipmentCode es obligatorio") String equipmentCode,
        @NotNull(message = "startTime es obligatorio") @Future(message = "startTime debe ser futuro") ZonedDateTime startTime,
        @NotNull(message = "endTime es obligatorio") ZonedDateTime endTime,
        @NotNull(message = "capacity es obligatorio") @Min(value = 1, message = "capacity debe ser al menos 1") Integer capacity) {
}
