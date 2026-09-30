package com.example.demo.repository;

import com.example.demo.entity.EquipmentSlot;
import com.example.demo.entity.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface EquipmentSlotRepository extends JpaRepository<EquipmentSlot, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<EquipmentSlot> {

    Page<EquipmentSlot> findByLaboratoryId(Long laboratoryId, Pageable pageable);

    Page<EquipmentSlot> findByLaboratoryIdAndStatus(Long laboratoryId, SlotStatus status, Pageable pageable);

    /** Hay otro turno no cancelado del mismo equipo que se solapa con [start, end)? */
    @Query("""
            select count(s) > 0 from EquipmentSlot s
            where s.laboratoryId = :labId and s.equipmentCode = :code
              and s.status <> com.example.demo.entity.SlotStatus.CANCELLED
              and s.startTime < :end and s.endTime > :start
            """)
    boolean existsOverlap(@Param("labId") Long laboratoryId, @Param("code") String equipmentCode,
                          @Param("start") ZonedDateTime start, @Param("end") ZonedDateTime end);

    /** Bloqueo pesimista: evita sobre-reservar el mismo turno con peticiones concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from EquipmentSlot s where s.id = :id")
    Optional<EquipmentSlot> findByIdForUpdate(@Param("id") Long id);
}
