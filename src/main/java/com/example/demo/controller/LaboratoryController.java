package com.example.demo.controller;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.*;
import com.example.demo.entity.LabStatus;
import com.example.demo.entity.SlotStatus;
import com.example.demo.service.EquipmentSlotService;
import com.example.demo.service.LaboratoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/laboratories")
@RequiredArgsConstructor
@Validated
public class LaboratoryController {

    private final LaboratoryService laboratoryService;
    private final EquipmentSlotService slotService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LaboratoryResponse create(@Valid @RequestBody LaboratoryRequest request) {
        return laboratoryService.create(request);
    }

    @GetMapping
    public PageResponse<LaboratoryResponse> list(@RequestParam(required = false) LabStatus status,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return laboratoryService.list(status, page, size);
    }

    @GetMapping("/{id}")
    public LaboratoryResponse get(@PathVariable Long id) {
        return laboratoryService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public LaboratoryResponse update(@PathVariable Long id, @Valid @RequestBody LaboratoryRequest request) {
        return laboratoryService.update(id, request);
    }

    /** Turnos publicados de un laboratorio. */
    @GetMapping("/{id}/slots")
    public PageResponse<SlotResponse> slots(@PathVariable Long id,
                                            @RequestParam(required = false) SlotStatus status,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return slotService.listByLaboratory(id, status, page, size);
    }
}
