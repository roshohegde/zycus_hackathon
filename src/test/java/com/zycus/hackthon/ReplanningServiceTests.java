package com.zycus.hackthon;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;
import com.zycus.hackthon.domain.Order;
import com.zycus.hackthon.domain.OrderStatus;
import com.zycus.hackthon.domain.ReassignmentSuggestion;
import com.zycus.hackthon.domain.SuggestionStatus;
import com.zycus.hackthon.domain.TriggerReason;
import com.zycus.hackthon.event.AgentOfflineEvent;
import com.zycus.hackthon.event.ReassignmentRejectedEvent;
import com.zycus.hackthon.repository.OrderRepository;
import com.zycus.hackthon.repository.SuggestionRepository;
import com.zycus.hackthon.routing.TriggerContext;
import com.zycus.hackthon.service.ReplanningService;
import com.zycus.hackthon.service.SuggestionService;

class ReplanningServiceTests {

    @Test
    void skipsExistingPendingOfflineSuggestion() {
        var orderRepository = Mockito.mock(OrderRepository.class);
        var suggestionRepository = Mockito.mock(SuggestionRepository.class);
        var suggestionService = Mockito.mock(SuggestionService.class);
        var service = new ReplanningService(orderRepository, suggestionRepository, suggestionService);
        var agent = new Agent("AGT-001", "Priya", AgentStatus.OFFLINE, 1);
        var order = new Order("ORD-001", "Documents", agent);
        when(orderRepository.findByAssignedAgentIdAndStatusIn("AGT-001",
                List.of(OrderStatus.ASSIGNED, OrderStatus.REASSIGNMENT_PENDING))).thenReturn(List.of(order));
        when(suggestionRepository.findByOrderIdAndStatusAndTriggerReason("ORD-001", SuggestionStatus.PENDING,
                TriggerReason.AGENT_OFFLINE)).thenReturn(Optional.of(Mockito.mock(ReassignmentSuggestion.class)));

        service.replan(new AgentOfflineEvent("AGT-001"));

        verify(suggestionService, never()).suggest(any(), any());
    }

    @Test
    void rejectionCreatesFreshPendingReplan() {
        var orderRepository = Mockito.mock(OrderRepository.class);
        var suggestionRepository = Mockito.mock(SuggestionRepository.class);
        var suggestionService = Mockito.mock(SuggestionService.class);
        var service = new ReplanningService(orderRepository, suggestionRepository, suggestionService);
        when(suggestionRepository.findByOrderIdAndStatusAndTriggerReason("ORD-001", SuggestionStatus.PENDING,
                TriggerReason.AGENT_OFFLINE)).thenReturn(Optional.empty());

        service.replanAfterRejection(new ReassignmentRejectedEvent("ORD-001", "AGT-001", "AGT-002"));

        verify(suggestionService).suggest("ORD-001",
            new TriggerContext(TriggerReason.AGENT_OFFLINE, "AGT-001", "AGT-002"));
    }
}