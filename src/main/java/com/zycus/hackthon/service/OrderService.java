package com.zycus.hackthon.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zycus.hackthon.api.CreateOrderRequest;
import com.zycus.hackthon.domain.Order;
import com.zycus.hackthon.domain.OrderStatus;
import com.zycus.hackthon.repository.AgentRepository;
import com.zycus.hackthon.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AgentRepository agentRepository;

    public OrderService(OrderRepository orderRepository, AgentRepository agentRepository) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
    }

    @Transactional
    public Order create(CreateOrderRequest request) {
        var agent = agentRepository.findById(request.agentId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + request.agentId()));
        agent.setActiveOrderCount(agent.getActiveOrderCount() + 1);
        return orderRepository.save(new Order(nextId(), request.description(), agent));
    }

    @Transactional(readOnly = true)
    public List<Order> find(OrderStatus status) {
        return status == null ? orderRepository.findAll() : orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Order get(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public long countAssignedOrders(String agentId, List<OrderStatus> statuses) {
        return orderRepository.countByAssignedAgentIdAndStatusIn(agentId, statuses);
    }

    private String nextId() {
        return "ORD-%03d".formatted(orderRepository.count() + 1);
    }
}