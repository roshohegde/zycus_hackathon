package com.zycus.hackthon;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.ApplicationEventPublisher;

import com.zycus.hackthon.api.UpdateAgentStatusRequest;
import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;
import com.zycus.hackthon.event.AgentOfflineEvent;
import com.zycus.hackthon.repository.AgentRepository;
import com.zycus.hackthon.repository.OrderRepository;
import com.zycus.hackthon.service.AgentService;

class AgentServiceTests {

    @Test
    void offlineStatusPublishesRecoveryEvent() {
        var repository = Mockito.mock(AgentRepository.class);
        var publisher = Mockito.mock(ApplicationEventPublisher.class);
        var orderRepository = Mockito.mock(OrderRepository.class);
        var agent = new Agent("AGT-001", "Priya", AgentStatus.BUSY, 2);
        when(repository.findById("AGT-001")).thenReturn(Optional.of(agent));
        var service = new AgentService(repository, publisher, orderRepository);

        service.updateStatus("AGT-001", new UpdateAgentStatusRequest(AgentStatus.OFFLINE));

        verify(publisher).publishEvent(new AgentOfflineEvent("AGT-001"));
    }
}