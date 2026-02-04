package com.example.omsv6.repository;

import com.example.omsv6.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findAllByOrderByCreatedAtDesc();
    List<OrderEntity> findByStatusOrderByCreatedAtDesc(String status);
    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<OrderEntity> findByStatusAndCustomerIdOrderByCreatedAtDesc(String status, String customerId);
    List<OrderEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
