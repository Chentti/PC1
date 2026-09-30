package com.example.demo.repository;

import com.example.demo.entity.LabStatus;
import com.example.demo.entity.Laboratory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {
    Page<Laboratory> findByStatus(LabStatus status, Pageable pageable);
}
