package com.example.demo.service;

import com.example.demo.common.TimeUtil;
import com.example.demo.dto.ReservationRequest;
import com.example.demo.dto.ReservationResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.repository.EquipmentSlotRepository;
import com.example.demo.repository.LabReservationRepository;
import com.example.demo.repository.LaboratoryRepository;
import com.example.demo.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabReservationServiceTest {

    @Mock LabReservationRepository reservationRepository;
    @Mock EquipmentSlotRepository slotRepository;
    @Mock LaboratoryRepository laboratoryRepository;
    @InjectMocks LabReservationService service;

    private EquipmentSlot slot;
    private Laboratory lab;

    @BeforeEach
    void setUp() {
        slot = new EquipmentSlot();
        slot.setId(1L);
        slot.setLaboratoryId(10L);
        slot.setEquipmentCode("MIC-01");
        slot.setStartTime(TimeUtil.now().plusDays(1));
        slot.setEndTime(TimeUtil.now().plusDays(1).plusHours(1));
        slot.setCapacity(2);
        slot.setStatus(SlotStatus.AVAILABLE);

        lab = new Laboratory();
        lab.setId(10L);
        lab.setStatus(LabStatus.ACTIVE);
    }

    private void stubHappyPath(long alreadyTaken) {
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));
        when(laboratoryRepository.findById(10L)).thenReturn(Optional.of(lab));
        when(reservationRepository.existsBySlotIdAndStudentIdAndStatusIn(anyLong(), anyLong(), anyCollection()))
                .thenReturn(false);
        when(reservationRepository.countBySlotIdAndStatusIn(anyLong(), anyCollection())).thenReturn(alreadyTaken);
        when(reservationRepository.save(any(LabReservation.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void create_reservaConCupo_quedaReservedYSlotSigueAvailable() {
        stubHappyPath(0);

        ReservationResponse res = service.create(5L, new ReservationRequest(1L, "Práctica de circuitos"));

        assertEquals(ReservationStatus.RESERVED, res.status());
        assertEquals(5L, res.studentId());
        assertNotNull(res.reservedAt());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
    }

    @Test
    void create_ultimoCupo_marcaSlotComoFull() {
        stubHappyPath(1);

        service.create(5L, new ReservationRequest(1L, "Práctica"));

        assertEquals(SlotStatus.FULL, slot.getStatus());
    }

    @Test
    void create_slotFull_lanzaConflict() {
        slot.setStatus(SlotStatus.FULL);
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        assertThrows(ConflictException.class, () -> service.create(5L, new ReservationRequest(1L, "x")));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void create_slotCancelado_lanzaConflict() {
        slot.setStatus(SlotStatus.CANCELLED);
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        assertThrows(ConflictException.class, () -> service.create(5L, new ReservationRequest(1L, "x")));
    }

    @Test
    void create_laboratorioEnMantenimiento_lanzaConflict() {
        lab.setStatus(LabStatus.MAINTENANCE);
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));
        when(laboratoryRepository.findById(10L)).thenReturn(Optional.of(lab));

        assertThrows(ConflictException.class, () -> service.create(5L, new ReservationRequest(1L, "x")));
    }

    @Test
    void create_reservaDuplicadaDelEstudiante_lanzaConflict() {
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));
        when(laboratoryRepository.findById(10L)).thenReturn(Optional.of(lab));
        when(reservationRepository.existsBySlotIdAndStudentIdAndStatusIn(anyLong(), anyLong(), anyCollection()))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(5L, new ReservationRequest(1L, "x")));
    }

    @Test
    void create_turnoYaComenzado_lanzaConflict() {
        slot.setStartTime(TimeUtil.now().minusHours(1));
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        assertThrows(ConflictException.class, () -> service.create(5L, new ReservationRequest(1L, "x")));
    }

    @Test
    void cancel_reservaPropia_liberaCupoDeSlotFull() {
        slot.setStatus(SlotStatus.FULL);
        LabReservation r = reservation(5L, ReservationStatus.RESERVED);
        when(reservationRepository.findById(7L)).thenReturn(Optional.of(r));
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        ReservationResponse res = service.cancel(7L, new AuthUser(5L, "ana", "ROLE_STUDENT"));

        assertEquals(ReservationStatus.CANCELLED, res.status());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
    }

    @Test
    void cancel_reservaDeOtroEstudiante_lanzaForbidden() {
        when(reservationRepository.findById(7L))
                .thenReturn(Optional.of(reservation(5L, ReservationStatus.RESERVED)));

        assertThrows(ForbiddenException.class,
                () -> service.cancel(7L, new AuthUser(99L, "otro", "ROLE_STUDENT")));
    }

    @Test
    void cancel_adminPuedeCancelarReservaAjena() {
        when(reservationRepository.findById(7L))
                .thenReturn(Optional.of(reservation(5L, ReservationStatus.RESERVED)));
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        ReservationResponse res = service.cancel(7L, new AuthUser(1L, "admin", "ROLE_ADMIN"));

        assertEquals(ReservationStatus.CANCELLED, res.status());
    }

    @Test
    void cancel_reservaYaUsada_lanzaConflict() {
        when(reservationRepository.findById(7L))
                .thenReturn(Optional.of(reservation(5L, ReservationStatus.USED)));

        assertThrows(ConflictException.class,
                () -> service.cancel(7L, new AuthUser(5L, "ana", "ROLE_STUDENT")));
    }

    @Test
    void markUsed_reservaReserved_pasaAUsed() {
        when(reservationRepository.findById(7L))
                .thenReturn(Optional.of(reservation(5L, ReservationStatus.RESERVED)));

        assertEquals(ReservationStatus.USED, service.markUsed(7L).status());
    }

    private LabReservation reservation(Long studentId, ReservationStatus status) {
        LabReservation r = new LabReservation();
        r.setId(7L);
        r.setSlotId(1L);
        r.setStudentId(studentId);
        r.setPurpose("x");
        r.setReservedAt(TimeUtil.now());
        r.setStatus(status);
        return r;
    }
}
