package com.zycus.hackthon.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zycus.hackthon.api.UpdateAgentStatusRequest;
import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;
import com.zycus.hackthon.event.AgentOfflineEvent;
import com.zycus.hackthon.repository.AgentRepository;
import com.zycus.hackthon.repository.OrderRepository;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderRepository orderRepository;

    public AgentService(AgentRepository agentRepository, ApplicationEventPublisher eventPublisher,
            OrderRepository orderRepository) {
        this.agentRepository = agentRepository;
        this.eventPublisher = eventPublisher;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Agent> findAll() {
        var activeStatuses = List.of(com.zycus.hackthon.domain.OrderStatus.ASSIGNED,
            com.zycus.hackthon.domain.OrderStatus.REASSIGNMENT_PENDING,
            com.zycus.hackthon.domain.OrderStatus.REASSIGNED);
        return agentRepository.findAll().stream().peek(agent -> agent.setActiveOrderCount(
            Math.toIntExact(orderRepository.countByAssignedAgentIdAndStatusIn(agent.getId(), activeStatuses)))).toList();
    }

    @Transactional
    public Agent updateStatus(String id, UpdateAgentStatusRequest request) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
        agent.setStatus(request.status());
        if (request.status() == AgentStatus.OFFLINE) {
            eventPublisher.publishEvent(new AgentOfflineEvent(id));
        }
        return agent;
    }
}