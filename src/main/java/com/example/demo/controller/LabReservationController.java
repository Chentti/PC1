package com.example.demo.controller;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.ReservationRequest;
import com.example.demo.dto.ReservationResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.LabReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Validated
public class LabReservationController {

    private final LabReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody ReservationRequest request) {
        return reservationService.create(me.id(), request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public PageResponse<ReservationResponse> mine(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return reservationService.mine(me.id(), page, size);
    }

    @PatchMapping("/{id}/cancel")
    public ReservationResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return reservationService.cancel(id, me);
    }

    @PatchMapping("/{id}/use")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ReservationResponse markUsed(@PathVariable Long id) {
        return reservationService.markUsed(id);
    }
}
