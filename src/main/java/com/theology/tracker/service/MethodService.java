package com.theology.tracker.service;

import com.theology.tracker.dto.MethodFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.Method;
import com.theology.tracker.model.StudySession;
import com.theology.tracker.repository.MethodRepository;
import com.theology.tracker.repository.PracticeSessionItemRepository;
import com.theology.tracker.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MethodService {

    private final MethodRepository methodRepo;
    private final StudySessionRepository studySessionRepo;
    private final PracticeSessionItemRepository practiceSessionItemRepo;

    public MethodService(
        MethodRepository methodRepo,
        StudySessionRepository studySessionRepo,
        PracticeSessionItemRepository practiceSessionItemRepo
    ) {
        this.methodRepo = methodRepo;
        this.studySessionRepo = studySessionRepo;
        this.practiceSessionItemRepo = practiceSessionItemRepo;
    }

    public List<Method> findAll() {
        return methodRepo.findAllByOrderByNameAsc();
    }

    public Method findById(Long id) {
        return methodRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Method not found: " + id));
    }

    public List<StudySession> findSessions(Long methodId) {
        return studySessionRepo.findByMethodIdOrderBySessionDateDesc(methodId);
    }

    public long countUsages(Long methodId) {
        return studySessionRepo.findByMethodIdOrderBySessionDateDesc(methodId).size();
    }

    @Transactional
    public Method create(MethodFormDto form) {
        validate(form);
        Method method = new Method();
        applyForm(method, form);
        return methodRepo.save(method);
    }

    @Transactional
    public Method update(Long id, MethodFormDto form) {
        validate(form);
        Method method = findById(id);
        applyForm(method, form);
        return methodRepo.save(method);
    }

    @Transactional
    public void delete(Long id) {
        Method method = findById(id);
        studySessionRepo.findByMethodIdOrderBySessionDateDesc(id)
            .forEach(s -> s.setMethod(null));
        practiceSessionItemRepo.findByMethodId(id)
            .forEach(p -> p.setMethod(null));
        methodRepo.delete(method);
    }

    private void validate(MethodFormDto form) {
        if (form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Method name is required.");
        }
    }

    private void applyForm(Method method, MethodFormDto form) {
        method.setName(form.name().trim());
        method.setDescription(form.description() != null ? form.description().trim() : null);
        method.setPersonalNotes(form.personalNotes() != null ? form.personalNotes().trim() : null);
    }
}
