package com.example.orderservice.repository;

import com.example.orderservice.entity.ReturnStateChange;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnStateChangeRepository extends JpaRepository<ReturnStateChange, Long> {
    List<ReturnStateChange> findByReturnId(UUID returnId);
}
