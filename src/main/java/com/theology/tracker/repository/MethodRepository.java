package com.theology.tracker.repository;

import com.theology.tracker.model.Method;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MethodRepository extends JpaRepository<Method, Long> {

    List<Method> findAllByOrderByNameAsc();
}
