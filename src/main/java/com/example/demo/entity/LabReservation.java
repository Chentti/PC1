package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "lab_reservations")
@Getter
@Setter
@NoArgsConstructor
public class LabReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "slot_id", nullable = false)
    private Long slotId;


    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 250)
    private String purpose;

    @Column(name = "reserved_at", nullable = false)
    private ZonedDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.RESERVED;
}
