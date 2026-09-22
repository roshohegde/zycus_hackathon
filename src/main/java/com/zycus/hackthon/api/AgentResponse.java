package com.zycus.hackthon.api;

import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;

public record AgentResponse(String id, String name, AgentStatus status, int activeOrderCount,
        String currentZone, Integer maxCapacity) {

    public static AgentResponse from(Agent agent) {
        return new AgentResponse(agent.getId(), agent.getName(), agent.getStatus(),
                agent.getActiveOrderCount(), agent.getCurrentZone(), agent.getMaxCapacity());
    }
}