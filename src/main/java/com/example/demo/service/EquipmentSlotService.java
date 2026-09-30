package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.SlotRequest;
import com.example.demo.dto.SlotResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.EquipmentSlotRepository;
import com.example.demo.repository.LabReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EquipmentSlotService {

    private final EquipmentSlotRepository slotRepository;
    private final LabReservationRepository reservationRepository;
    private final LaboratoryService laboratoryService;

    @Transactional
    public SlotResponse create(SlotRequest req) {
        Laboratory lab = laboratoryService.find(req.laboratoryId());
        if (lab.getStatus() != LabStatus.ACTIVE)
            throw new ConflictException("El laboratorio está en mantenimiento");
        if (!req.endTime().isAfter(req.startTime()))
            throw new BadRequestException("endTime debe ser posterior a startTime");
        if (slotRepository.existsOverlap(lab.getId(), req.equipmentCode(), req.startTime(), req.endTime()))
            throw new ConflictException("Ya existe un turno de ese equipo que se solapa en ese horario");

        EquipmentSlot slot = new EquipmentSlot();
        slot.setLaboratoryId(lab.getId());
        slot.setEquipmentCode(req.equipmentCode());
        slot.setStartTime(req.startTime());
        slot.setEndTime(req.endTime());
        slot.setCapacity(req.capacity());
        slot.setStatus(SlotStatus.AVAILABLE);
        return SlotResponse.from(slotRepository.save(slot));
    }

    @Transactional(readOnly = true)
    public PageResponse<SlotResponse> listByLaboratory(Long laboratoryId, SlotStatus status, int page, int size) {
        laboratoryService.find(laboratoryId);
        PageRequest pr = PageRequest.of(page, size, Sort.by("startTime"));
        Page<EquipmentSlot> result = status == null
                ? slotRepository.findByLaboratoryId(laboratoryId, pr)
                : slotRepository.findByLaboratoryIdAndStatus(laboratoryId, status, pr);
        return PageResponse.of(result, SlotResponse::from);
    }


    @Transactional(readOnly = true)
    public PageResponse<SlotResponse> search(Long laboratoryId, String equipmentCode,
                                             java.time.ZonedDateTime from, int page, int size) {
        org.springframework.data.jpa.domain.Specification<EquipmentSlot> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("status"), SlotStatus.AVAILABLE));
            predicates.add(cb.greaterThan(root.<java.time.ZonedDateTime>get("startTime"),
                    com.example.demo.common.TimeUtil.now()));
            if (laboratoryId != null) predicates.add(cb.equal(root.get("laboratoryId"), laboratoryId));
            if (equipmentCode != null && !equipmentCode.isBlank())
                predicates.add(cb.equal(root.get("equipmentCode"), equipmentCode));
            if (from != null)
                predicates.add(cb.greaterThanOrEqualTo(root.<java.time.ZonedDateTime>get("startTime"), from));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<EquipmentSlot> result = slotRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by("startTime").and(Sort.by("id"))));
        return PageResponse.of(result, SlotResponse::from);
    }

    @Transactional(readOnly = true)
    public SlotResponse get(Long id) {
        return SlotResponse.from(slotRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Turno no encontrado")));
    }

    /** Cancela el turno y las reservas activas que tenía. */
    @Transactional
    public SlotResponse cancel(Long id) {
        EquipmentSlot slot = slotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Turno no encontrado"));
        if (slot.getStatus() == SlotStatus.CANCELLED)
            throw new ConflictException("El turno ya está cancelado");
        slot.setStatus(SlotStatus.CANCELLED);
        reservationRepository.findBySlotIdAndStatus(id, ReservationStatus.RESERVED)
                .forEach(r -> r.setStatus(ReservationStatus.CANCELLED));
        return SlotResponse.from(slot);
    }
}
