package com.example.demo.repository;

import com.example.demo.entity.LabReservation;
import com.example.demo.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LabReservationRepository extends JpaRepository<LabReservation, Long> {

    Page<LabReservation> findByStudentId(Long studentId, Pageable pageable);

    long countBySlotIdAndStatusIn(Long slotId, Collection<ReservationStatus> statuses);

    boolean existsBySlotIdAndStudentIdAndStatusIn(Long slotId, Long studentId, Collection<ReservationStatus> statuses);

    List<LabReservation> findBySlotIdAndStatus(Long slotId, ReservationStatus status);
}
