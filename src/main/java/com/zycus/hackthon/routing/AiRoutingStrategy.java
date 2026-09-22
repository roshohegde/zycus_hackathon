package com.zycus.hackthon.routing;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zycus.hackthon.ai.LlmGateway;
import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.Order;

import tools.jackson.databind.ObjectMapper;

@Component("ai")
public class AiRoutingStrategy implements RoutingStrategy {

    private static final Logger log = LoggerFactory.getLogger(AiRoutingStrategy.class);

    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;
    private final RuleBasedRoutingStrategy fallback;

    public AiRoutingStrategy(LlmGateway llmGateway, ObjectMapper objectMapper,
            RuleBasedRoutingStrategy fallback) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
    }

    @Override
    public RoutingDecision recommend(Order order, List<Agent> availableAgents, TriggerContext context) {
        try {
            String response = llmGateway.call(buildPrompt(order, availableAgents, context));
            AiResponse aiResponse = objectMapper.readValue(stripMarkdownFence(response), AiResponse.class);
            Agent agent = availableAgents.stream()
                    .filter(candidate -> candidate.getId().equals(aiResponse.agentId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("AI selected unknown agent: " + aiResponse.agentId()));
            if (aiResponse.confidence() < 0.0 || aiResponse.confidence() > 1.0
                    || aiResponse.reasoning() == null || aiResponse.reasoning().isBlank()) {
                throw new IllegalArgumentException("AI response failed validation");
            }
            return new RoutingDecision(agent, aiResponse.confidence(), aiResponse.reasoning());
        } catch (Exception exception) {
            log.warn("AI routing failed for order {}; using rule-based fallback: {}",
                    order.getId(), exception.getMessage());
            RoutingDecision decision = fallback.recommend(order, availableAgents, context);
            return new RoutingDecision(decision.recommendedAgent(), decision.confidence(),
                    "AI unavailable; fallback recommendation. " + decision.reasoning());
        }
    }

    private String buildPrompt(Order order, List<Agent> availableAgents, TriggerContext context) {
        String roster = availableAgents.stream()
                .map(agent -> "{agentId=%s,name=%s,status=%s,activeOrderCount=%d}"
                        .formatted(agent.getId(), agent.getName(), agent.getStatus(), agent.getActiveOrderCount()))
                .collect(Collectors.joining(", "));
        String situation = context.reason().name().equals("AGENT_OFFLINE")
                ? "Recovery after agent %s went offline. Re-plan this stranded order and do not select that agent."
                        .formatted(context.failedAgentId())
                : "Initial routing suggestion for an order that needs an assignment.";
        if (context.excludedAgentId() != null) {
            situation += " The previous recommendation " + context.excludedAgentId()
                + " was rejected; choose a different available agent if possible.";
        }
        return "You are a delivery routing advisor. " + situation
                + "\nOrder: id=" + order.getId() + ", description=" + order.getDescription()
                + "\nAvailable agents: [" + roster + "]"
                + "\nReturn only valid JSON with this exact shape: "
                + "{\"agentId\":\"AGT-001\",\"confidence\":0.85,\"reasoning\":\"plain English explanation\"}.";
    }

    private String stripMarkdownFence(String response) {
        String value = response.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        return value.trim();
    }

    private record AiResponse(String agentId, double confidence, String reasoning) {
    }
}