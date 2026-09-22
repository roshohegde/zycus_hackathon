package com.zycus.hackthon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zycus.hackthon.domain.Order;
import com.zycus.hackthon.domain.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByAssignedAgentIdAndStatusIn(String agentId, List<OrderStatus> statuses);

    long countByAssignedAgentIdAndStatusIn(String agentId, List<OrderStatus> statuses);
}