package com.zycus.hackthon.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.zycus.hackthon.domain.OrderStatus;
import com.zycus.hackthon.domain.SuggestionStatus;
import com.zycus.hackthon.domain.TriggerReason;
import com.zycus.hackthon.event.AgentOfflineEvent;
import com.zycus.hackthon.event.ReassignmentRejectedEvent;
import com.zycus.hackthon.repository.OrderRepository;
import com.zycus.hackthon.repository.SuggestionRepository;
import com.zycus.hackthon.routing.TriggerContext;

@Service
public class ReplanningService {

    private static final Logger log = LoggerFactory.getLogger(ReplanningService.class);

    private final OrderRepository orderRepository;
    private final SuggestionRepository suggestionRepository;
    private final SuggestionService suggestionService;

    public ReplanningService(OrderRepository orderRepository, SuggestionRepository suggestionRepository,
            SuggestionService suggestionService) {
        this.orderRepository = orderRepository;
        this.suggestionRepository = suggestionRepository;
        this.suggestionService = suggestionService;
    }

    @Async("replanningExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void replan(AgentOfflineEvent event) {
        var affectedOrders = orderRepository.findByAssignedAgentIdAndStatusIn(event.agentId(),
                List.of(OrderStatus.ASSIGNED, OrderStatus.REASSIGNMENT_PENDING, OrderStatus.REASSIGNED));
        for (var order : affectedOrders) {
            var existing = suggestionRepository.findByOrderIdAndStatusAndTriggerReason(
                    order.getId(), SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE);
            if (existing.isPresent()) {
                continue;
            }
            try {
                suggestionService.suggest(order.getId(),
                        new TriggerContext(TriggerReason.AGENT_OFFLINE, event.agentId()));
            } catch (RuntimeException exception) {
                log.error("Re-planning failed for order {} after agent {} went offline",
                        order.getId(), event.agentId(), exception);
            }
        }
    }

    @Async("replanningExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void replanAfterRejection(ReassignmentRejectedEvent event) {
        if (suggestionRepository.findByOrderIdAndStatusAndTriggerReason(
                event.orderId(), SuggestionStatus.PENDING, TriggerReason.AGENT_OFFLINE).isPresent()) {
            return;
        }
        try {
            suggestionService.suggest(event.orderId(),
                    new TriggerContext(TriggerReason.AGENT_OFFLINE, event.failedAgentId(), event.rejectedAgentId()));
        } catch (RuntimeException exception) {
            log.error("Re-planning failed for rejected order {} after agent {} went offline",
                    event.orderId(), event.failedAgentId(), exception);
        }
    }
}