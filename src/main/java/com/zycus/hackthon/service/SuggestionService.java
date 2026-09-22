package com.zycus.hackthon.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zycus.hackthon.api.UpdateSuggestionRequest;
import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;
import com.zycus.hackthon.domain.Order;
import com.zycus.hackthon.domain.OrderStatus;
import com.zycus.hackthon.domain.ReassignmentSuggestion;
import com.zycus.hackthon.domain.SuggestionStatus;
import com.zycus.hackthon.event.ReassignmentRejectedEvent;
import com.zycus.hackthon.repository.AgentRepository;
import com.zycus.hackthon.repository.SuggestionRepository;
import com.zycus.hackthon.routing.RoutingDecision;
import com.zycus.hackthon.routing.RoutingStrategySelector;
import com.zycus.hackthon.routing.TriggerContext;

@Service
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final AgentRepository agentRepository;
    private final OrderService orderService;
    private final RoutingStrategySelector strategySelector;
        private final ApplicationEventPublisher eventPublisher;

    public SuggestionService(SuggestionRepository suggestionRepository, AgentRepository agentRepository,
            OrderService orderService, RoutingStrategySelector strategySelector,
            ApplicationEventPublisher eventPublisher) {
        this.suggestionRepository = suggestionRepository;
        this.agentRepository = agentRepository;
        this.orderService = orderService;
        this.strategySelector = strategySelector;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReassignmentSuggestion suggest(String orderId, TriggerContext context) {
        Order order = orderService.get(orderId);
        List<Agent> available = agentRepository.findByStatusIn(List.of(AgentStatus.AVAILABLE, AgentStatus.BUSY))
            .stream()
            .filter(agent -> !agent.getId().equals(context.excludedAgentId()))
            .toList();
        List<OrderStatus> activeStatuses = List.of(OrderStatus.ASSIGNED,
            OrderStatus.REASSIGNMENT_PENDING, OrderStatus.REASSIGNED);
        available.forEach(agent -> agent.setActiveOrderCount(Math.toIntExact(
            orderService.countAssignedOrders(agent.getId(), activeStatuses))));
        RoutingDecision decision = strategySelector.active().recommend(order, available, context);
        order.setStatus(com.zycus.hackthon.domain.OrderStatus.REASSIGNMENT_PENDING);
        return suggestionRepository.save(new ReassignmentSuggestion(order, decision.recommendedAgent(),
                decision.confidence(), decision.reasoning(), context.reason()));
    }

    @Transactional(readOnly = true)
    public List<ReassignmentSuggestion> find(SuggestionStatus status) {
        return status == null ? suggestionRepository.findAll() : suggestionRepository.findAll().stream()
                .filter(suggestion -> suggestion.getStatus() == status).toList();
    }

    @Transactional
    public ReassignmentSuggestion update(Long id, UpdateSuggestionRequest request) {
        ReassignmentSuggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Suggestion not found: " + id));
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is already resolved: " + id);
        }
        suggestion.setStatus(request.status());
        if (request.status() == SuggestionStatus.ACCEPTED) {
            Order order = suggestion.getOrder();
            Agent previousAgent = order.getAssignedAgent();
            Agent replacementAgent = suggestion.getRecommendedAgent();
            if (previousAgent != replacementAgent) {
                previousAgent.setActiveOrderCount(Math.max(0, previousAgent.getActiveOrderCount() - 1));
                replacementAgent.setActiveOrderCount(replacementAgent.getActiveOrderCount() + 1);
                order.setAssignedAgent(replacementAgent);
            }
            order.setStatus(com.zycus.hackthon.domain.OrderStatus.REASSIGNED);
        } else if (request.status() == SuggestionStatus.REJECTED) {
            Order order = suggestion.getOrder();
            order.setStatus(com.zycus.hackthon.domain.OrderStatus.REASSIGNMENT_PENDING);
            eventPublisher.publishEvent(new ReassignmentRejectedEvent(order.getId(),
                    order.getAssignedAgent().getId(), suggestion.getRecommendedAgent().getId()));
        }
        return suggestion;
    }
}