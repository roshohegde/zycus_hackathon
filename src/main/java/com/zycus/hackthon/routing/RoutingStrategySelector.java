package com.zycus.hackthon.routing;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RoutingStrategySelector {

    private final Map<String, RoutingStrategy> strategies;
    private final AtomicReference<String> activeStrategy;

    public RoutingStrategySelector(Map<String, RoutingStrategy> strategies,
            @Value("${routing.strategy:rule-based}") String configuredStrategy) {
        this.strategies = strategies;
        this.activeStrategy = new AtomicReference<>(configuredStrategy);
    }

    public RoutingStrategy active() {
        RoutingStrategy strategy = strategies.get(activeStrategy.get());
        if (strategy == null) {
            throw new IllegalStateException("Unknown routing strategy: " + activeStrategy.get());
        }
        return strategy;
    }

    public String currentName() {
        return activeStrategy.get();
    }

    public void select(String strategyName) {
        if (!strategies.containsKey(strategyName)) {
            throw new IllegalArgumentException("Unknown routing strategy: " + strategyName);
        }
        activeStrategy.set(strategyName);
    }
}