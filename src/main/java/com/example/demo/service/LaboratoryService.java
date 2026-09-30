package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.LaboratoryRequest;
import com.example.demo.dto.LaboratoryResponse;
import com.example.demo.entity.LabStatus;
import com.example.demo.entity.Laboratory;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.LaboratoryRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public LaboratoryResponse create(LaboratoryRequest req) {
        Laboratory lab = new Laboratory();
        apply(lab, req);
        return LaboratoryResponse.from(laboratoryRepository.save(lab));
    }

    @Transactional(readOnly = true)
    public PageResponse<LaboratoryResponse> list(LabStatus status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("id"));
        Page<Laboratory> result = status == null
                ? laboratoryRepository.findAll(pr)
                : laboratoryRepository.findByStatus(status, pr);
        return PageResponse.of(result, LaboratoryResponse::from);
    }

    @Transactional(readOnly = true)
    public LaboratoryResponse get(Long id) {
        return LaboratoryResponse.from(find(id));
    }

    @Transactional
    public LaboratoryResponse update(Long id, LaboratoryRequest req) {
        Laboratory lab = find(id);
        apply(lab, req);
        return LaboratoryResponse.from(lab);
    }

    public Laboratory find(Long id) {
        return laboratoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Laboratorio no encontrado"));
    }

    private void apply(Laboratory lab, LaboratoryRequest req) {
        User manager = userRepository.findById(req.managerId())
                .orElseThrow(() -> new NotFoundException("El responsable (managerId) no existe"));
        if (manager.getRole() == Role.STUDENT)
            throw new BadRequestException("El responsable debe ser TECHNICIAN o ADMIN");

        lab.setUsername(req.username());
        lab.setEmail(req.email());
        lab.setLocation(req.location());
        lab.setManager(manager);
        if (req.status() != null) lab.setStatus(req.status());
    }
}
