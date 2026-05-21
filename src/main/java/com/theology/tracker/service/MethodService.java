package com.theology.tracker.service;

import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.Method;
import com.theology.tracker.repository.MethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MethodService {

    private final MethodRepository methodRepo;

    public MethodService(MethodRepository methodRepo) {
        this.methodRepo = methodRepo;
    }

    public List<Method> findAll() {
        return methodRepo.findAllByOrderByNameAsc();
    }

    public Method findById(Long id) {
        return methodRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Method not found: " + id));
    }
}
