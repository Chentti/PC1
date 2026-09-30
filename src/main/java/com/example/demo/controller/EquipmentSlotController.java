package com.example.demo.controller;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.SlotRequest;
import com.example.demo.dto.SlotResponse;
import com.example.demo.service.EquipmentSlotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/slots")
@RequiredArgsConstructor
@Validated
public class EquipmentSlotController {

    private final EquipmentSlotService slotService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(@Valid @RequestBody SlotRequest request) {
        return slotService.create(request);
    }

    /**
     * Búsqueda de turnos AVAILABLE y futuros; filtros combinables.
     * Ej: /slots?laboratoryId=1&equipmentCode=MIC-01&from=2026-10-05T08:00:00-05:00&page=0&size=10
     */
    @GetMapping
    public PageResponse<SlotResponse> search(@RequestParam(required = false) Long laboratoryId,
                                             @RequestParam(required = false) String equipmentCode,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return slotService.search(laboratoryId, equipmentCode, from, page, size);
    }

    @GetMapping("/{id}")
    public SlotResponse get(@PathVariable Long id) {
        return slotService.get(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public SlotResponse cancel(@PathVariable Long id) {
        return slotService.cancel(id);
    }
}
