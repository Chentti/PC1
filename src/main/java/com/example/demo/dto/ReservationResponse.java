package com.example.demo.dto;

import com.example.demo.entity.LabReservation;
import com.example.demo.entity.ReservationStatus;

import java.time.ZonedDateTime;

public record ReservationResponse(Long id, Long slotId, Long studentId, String purpose,
                                  ZonedDateTime reservedAt, ReservationStatus status) {
    public static ReservationResponse from(LabReservation r) {
        return new ReservationResponse(r.getId(), r.getSlotId(), r.getStudentId(), r.getPurpose(),
                r.getReservedAt(), r.getStatus());
    }
}
