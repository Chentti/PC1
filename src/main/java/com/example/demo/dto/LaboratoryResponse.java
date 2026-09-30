package com.example.demo.dto;

import com.example.demo.entity.LabStatus;
import com.example.demo.entity.Laboratory;

public record LaboratoryResponse(Long id, String username, String email, String location,
                                 Long managerId, LabStatus status) {
    public static LaboratoryResponse from(Laboratory l) {
        return new LaboratoryResponse(l.getId(), l.getUsername(), l.getEmail(), l.getLocation(),
                l.getManager().getId(), l.getStatus());
    }
}
