package com.zycus.hackthon.routing;

import com.zycus.hackthon.domain.Agent;

public record RoutingDecision(Agent recommendedAgent, double confidence, String reasoning) {
}