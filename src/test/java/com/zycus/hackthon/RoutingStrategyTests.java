package com.zycus.hackthon;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import com.zycus.hackthon.ai.LlmGateway;
import com.zycus.hackthon.ai.StubLlmGateway;
import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;
import com.zycus.hackthon.domain.Order;
import com.zycus.hackthon.routing.AiRoutingStrategy;
import com.zycus.hackthon.routing.RoutingStrategy;
import com.zycus.hackthon.routing.RoutingStrategySelector;
import com.zycus.hackthon.routing.RuleBasedRoutingStrategy;
import com.zycus.hackthon.routing.TriggerContext;

import tools.jackson.databind.json.JsonMapper;

class RoutingStrategyTests {

    private final Order order = new Order("ORD-001", "Documents", new Agent("AGT-999", "Old", AgentStatus.OFFLINE, 1));

    @Test
    void ruleBasedSelectsLowestLoadAndBreaksTiesById() {
        var strategy = new RuleBasedRoutingStrategy();
        var decision = strategy.recommend(order, List.of(
                new Agent("AGT-003", "Busy", AgentStatus.BUSY, 1),
                new Agent("AGT-002", "Available", AgentStatus.AVAILABLE, 1)),
                TriggerContext.initial());

        assertEquals("AGT-002", decision.recommendedAgent().getId());
    }

    @Test
    void aiAcceptsValidStructuredResponse() {
        var gateway = mock(LlmGateway.class);
        org.mockito.Mockito.when(gateway.call(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("{\"agentId\":\"AGT-002\",\"confidence\":0.91,\"reasoning\":\"Lowest load\"}");
        var strategy = new AiRoutingStrategy(gateway, JsonMapper.builder().build(), new RuleBasedRoutingStrategy());

        var decision = strategy.recommend(order,
                List.of(new Agent("AGT-002", "Available", AgentStatus.AVAILABLE, 0)), TriggerContext.initial());

        assertEquals("AGT-002", decision.recommendedAgent().getId());
        assertEquals(0.91, decision.confidence());
        assertEquals("Lowest load", decision.reasoning());
    }

    @Test
    void aiFallsBackForHallucinatedAgent() {
        var gateway = mock(LlmGateway.class);
        org.mockito.Mockito.when(gateway.call(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("{\"agentId\":\"AGT-404\",\"confidence\":0.99,\"reasoning\":\"Unknown\"}");
        var strategy = new AiRoutingStrategy(gateway, JsonMapper.builder().build(), new RuleBasedRoutingStrategy());

        var decision = strategy.recommend(order,
                List.of(new Agent("AGT-002", "Available", AgentStatus.AVAILABLE, 0)), TriggerContext.initial());

        assertEquals("AGT-002", decision.recommendedAgent().getId());
        assertTrue(decision.reasoning().startsWith("AI unavailable; fallback recommendation."));
    }

        @Test
        void aiFallsBackForMalformedJsonAndInvalidConfidence() {
        var gateway = mock(LlmGateway.class);
        org.mockito.Mockito.when(gateway.call(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn("not-json");
        var strategy = new AiRoutingStrategy(gateway, JsonMapper.builder().build(), new RuleBasedRoutingStrategy());
        var malformed = strategy.recommend(order,
            List.of(new Agent("AGT-002", "Available", AgentStatus.AVAILABLE, 0)), TriggerContext.initial());
        assertEquals("AGT-002", malformed.recommendedAgent().getId());

        org.mockito.Mockito.when(gateway.call(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn("{\"agentId\":\"AGT-002\",\"confidence\":1.4,\"reasoning\":\"Too high\"}");
        var invalidConfidence = strategy.recommend(order,
            List.of(new Agent("AGT-002", "Available", AgentStatus.AVAILABLE, 0)), TriggerContext.initial());
        assertTrue(invalidConfidence.reasoning().startsWith("AI unavailable; fallback recommendation."));
        }

        @Test
        void ruleBasedRejectsEmptyRoster() {
        var strategy = new RuleBasedRoutingStrategy();
        assertThrows(IllegalStateException.class,
            () -> strategy.recommend(order, List.of(), TriggerContext.initial()));
        }

    @Test
    void selectorCanSwitchWithoutRestart() {
        RoutingStrategy rule = mock(RoutingStrategy.class);
        RoutingStrategy ai = mock(RoutingStrategy.class);
        var selector = new RoutingStrategySelector(Map.of("rule-based", rule, "ai", ai), "rule-based");

        selector.select("ai");

        assertEquals("ai", selector.currentName());
        assertEquals(ai, selector.active());
        assertThrows(IllegalArgumentException.class, () -> selector.select("missing"));
    }

    @Test
    void stubConfidenceReflectsSelectedAgentLoad() {
        var gateway = new StubLlmGateway();
        String response = gateway.call("Available agents: [{agentId=AGT-002,name=Rahul,status=AVAILABLE,activeOrderCount=0}, "
                + "{agentId=AGT-005,name=Deepak,status=BUSY,activeOrderCount=3}]");

        assertTrue(response.contains("\"agentId\":\"AGT-002\""));
        assertTrue(response.contains("\"confidence\":0.94"));
    }
}