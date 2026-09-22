package com.zycus.hackthon;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.ApplicationEventPublisher;

import com.zycus.hackthon.api.UpdateSuggestionRequest;
import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;
import com.zycus.hackthon.domain.Order;
import com.zycus.hackthon.domain.OrderStatus;
import com.zycus.hackthon.domain.ReassignmentSuggestion;
import com.zycus.hackthon.domain.SuggestionStatus;
import com.zycus.hackthon.domain.TriggerReason;
import com.zycus.hackthon.event.ReassignmentRejectedEvent;
import com.zycus.hackthon.repository.AgentRepository;
import com.zycus.hackthon.repository.SuggestionRepository;
import com.zycus.hackthon.routing.RoutingStrategySelector;
import com.zycus.hackthon.service.OrderService;
import com.zycus.hackthon.service.SuggestionService;

class SuggestionServiceTests {

    @Test
    void acceptingSuggestionReassignsOrder() {
        var previousAgent = new Agent("AGT-001", "Old", AgentStatus.OFFLINE, 1);
        var order = new Order("ORD-001", "Documents", previousAgent);
        var replacement = new Agent("AGT-002", "New", AgentStatus.AVAILABLE, 0);
        var suggestion = new ReassignmentSuggestion(order, replacement, 0.8, "Lowest load", TriggerReason.AGENT_OFFLINE);
        var repository = mock(SuggestionRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(suggestion));
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var service = new SuggestionService(repository, mock(AgentRepository.class), mock(OrderService.class),
            mock(RoutingStrategySelector.class), eventPublisher);

        service.update(1L, new UpdateSuggestionRequest(SuggestionStatus.ACCEPTED));

        assertEquals(replacement, order.getAssignedAgent());
        assertEquals(OrderStatus.REASSIGNED, order.getStatus());
        assertEquals(0, previousAgent.getActiveOrderCount());
        assertEquals(1, replacement.getActiveOrderCount());
    }

        @Test
        void rejectingSuggestionKeepsOrderPending() {
        var order = new Order("ORD-001", "Documents", new Agent("AGT-001", "Old", AgentStatus.OFFLINE, 1));
        var suggestion = new ReassignmentSuggestion(order,
            new Agent("AGT-002", "New", AgentStatus.AVAILABLE, 0), 0.8, "Lowest load",
            TriggerReason.AGENT_OFFLINE);
        var repository = mock(SuggestionRepository.class);
        when(repository.findById(2L)).thenReturn(Optional.of(suggestion));
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var service = new SuggestionService(repository, mock(AgentRepository.class), mock(OrderService.class),
            mock(RoutingStrategySelector.class), eventPublisher);

        service.update(2L, new UpdateSuggestionRequest(SuggestionStatus.REJECTED));

        assertEquals(SuggestionStatus.REJECTED, suggestion.getStatus());
        assertEquals(OrderStatus.REASSIGNMENT_PENDING, order.getStatus());
        verify(eventPublisher).publishEvent(new ReassignmentRejectedEvent("ORD-001", "AGT-001", "AGT-002"));
        }

        @Test
        void resolvedSuggestionCannotBeChangedAgain() {
        var order = new Order("ORD-001", "Documents", new Agent("AGT-001", "Old", AgentStatus.OFFLINE, 1));
        var suggestion = new ReassignmentSuggestion(order,
            new Agent("AGT-002", "New", AgentStatus.AVAILABLE, 0), 0.8, "Lowest load",
            TriggerReason.AGENT_OFFLINE);
        suggestion.setStatus(SuggestionStatus.REJECTED);
        var repository = mock(SuggestionRepository.class);
        when(repository.findById(3L)).thenReturn(Optional.of(suggestion));
        var service = new SuggestionService(repository, mock(AgentRepository.class), mock(OrderService.class),
            mock(RoutingStrategySelector.class), mock(ApplicationEventPublisher.class));

        assertThrows(IllegalStateException.class,
            () -> service.update(3L, new UpdateSuggestionRequest(SuggestionStatus.ACCEPTED)));
        }
}