package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.common.TimeUtil;
import com.example.demo.dto.ReservationRequest;
import com.example.demo.dto.ReservationResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.EquipmentSlotRepository;
import com.example.demo.repository.LabReservationRepository;
import com.example.demo.repository.LaboratoryRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabReservationService {


    private static final List<ReservationStatus> ACTIVE = List.of(ReservationStatus.RESERVED, ReservationStatus.USED);

    private final LabReservationRepository reservationRepository;
    private final EquipmentSlotRepository slotRepository;
    private final LaboratoryRepository laboratoryRepository;

    @Transactional
    public ReservationResponse create(Long studentId, ReservationRequest req) {
        EquipmentSlot slot = slotRepository.findByIdForUpdate(req.slotId())
                .orElseThrow(() -> new NotFoundException("Turno no encontrado"));

        if (slot.getStatus() == SlotStatus.CANCELLED)
            throw new ConflictException("El turno está cancelado");
        if (slot.getStatus() == SlotStatus.FULL)
            throw new ConflictException("El turno ya no tiene cupos");
        if (!slot.getStartTime().isAfter(TimeUtil.now()))
            throw new ConflictException("El turno ya comenzó o terminó");

        Laboratory lab = laboratoryRepository.findById(slot.getLaboratoryId())
                .orElseThrow(() -> new NotFoundException("Laboratorio no encontrado"));
        if (lab.getStatus() != LabStatus.ACTIVE)
            throw new ConflictException("El laboratorio está en mantenimiento");

        if (reservationRepository.existsBySlotIdAndStudentIdAndStatusIn(slot.getId(), studentId, ACTIVE))
            throw new ConflictException("Ya tienes una reserva para este turno");

        long taken = reservationRepository.countBySlotIdAndStatusIn(slot.getId(), ACTIVE);
        if (taken >= slot.getCapacity())
            throw new ConflictException("El turno ya no tiene cupos");

        LabReservation r = new LabReservation();
        r.setSlotId(slot.getId());
        r.setStudentId(studentId);
        r.setPurpose(req.purpose());
        r.setReservedAt(TimeUtil.now());
        r.setStatus(ReservationStatus.RESERVED);
        r = reservationRepository.save(r);

        if (taken + 1 >= slot.getCapacity()) slot.setStatus(SlotStatus.FULL);
        return ReservationResponse.from(r);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> mine(Long studentId, int page, int size) {
        return PageResponse.of(
                reservationRepository.findByStudentId(studentId, PageRequest.of(page, size, Sort.by("id").descending())),
                ReservationResponse::from);
    }


    @Transactional
    public ReservationResponse cancel(Long id, AuthUser me) {
        LabReservation r = find(id);
        boolean isAdmin = Role.ADMIN.authority().equals(me.role());
        if (!isAdmin && !r.getStudentId().equals(me.id()))
            throw new ForbiddenException("No puedes cancelar la reserva de otro usuario");
        if (r.getStatus() != ReservationStatus.RESERVED)
            throw new ConflictException("Solo se pueden cancelar reservas en estado RESERVED");

        r.setStatus(ReservationStatus.CANCELLED);
        slotRepository.findByIdForUpdate(r.getSlotId()).ifPresent(slot -> {
            if (slot.getStatus() == SlotStatus.FULL) slot.setStatus(SlotStatus.AVAILABLE);
        });
        return ReservationResponse.from(r);
    }


    @Transactional
    public ReservationResponse markUsed(Long id) {
        LabReservation r = find(id);
        if (r.getStatus() != ReservationStatus.RESERVED)
            throw new ConflictException("Solo se pueden marcar como usadas reservas en estado RESERVED");
        r.setStatus(ReservationStatus.USED);
        return ReservationResponse.from(r);
    }

    private LabReservation find(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
    }
}
