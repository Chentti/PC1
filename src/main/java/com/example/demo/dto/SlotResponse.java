package com.example.demo.dto;

import com.example.demo.entity.EquipmentSlot;
import com.example.demo.entity.SlotStatus;

import java.time.ZonedDateTime;

public record SlotResponse(Long id, Long laboratoryId, String equipmentCode, ZonedDateTime startTime,
                           ZonedDateTime endTime, Integer capacity, SlotStatus status) {
    public static SlotResponse from(EquipmentSlot s) {
        return new SlotResponse(s.getId(), s.getLaboratoryId(), s.getEquipmentCode(), s.getStartTime(),
                s.getEndTime(), s.getCapacity(), s.getStatus());
    }
}
