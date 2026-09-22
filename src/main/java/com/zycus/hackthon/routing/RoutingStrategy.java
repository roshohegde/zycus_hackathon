package com.zycus.hackthon.routing;

import java.util.List;

import com.zycus.hackthon.domain.Order;

public interface RoutingStrategy {

    RoutingDecision recommend(Order order, List<com.zycus.hackthon.domain.Agent> availableAgents,
            TriggerContext context);
}