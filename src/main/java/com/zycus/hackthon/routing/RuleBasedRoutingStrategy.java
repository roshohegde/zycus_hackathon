package com.zycus.hackthon.routing;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.Order;

@Component("rule-based")
public class RuleBasedRoutingStrategy implements RoutingStrategy {

    @Override
    public RoutingDecision recommend(Order order, List<Agent> availableAgents, TriggerContext context) {
        Agent agent = availableAgents.stream()
                .min(Comparator.comparingInt(Agent::getActiveOrderCount)
                        .thenComparing(Agent::getId))
                .orElseThrow(() -> new IllegalStateException("No available agent for order " + order.getId()));

        String reason = context.reason().name().equals("AGENT_OFFLINE")
                ? "Recovered the order after agent " + context.failedAgentId()
                        + "; selected " + agent.getId() + " with the lowest active load."
                : "Selected " + agent.getId() + " because it has the lowest active load.";
        return new RoutingDecision(agent, 0.70, reason);
    }
}