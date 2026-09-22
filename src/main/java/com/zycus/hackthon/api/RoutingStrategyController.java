package com.zycus.hackthon.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zycus.hackthon.routing.RoutingStrategySelector;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/routing")
public class RoutingStrategyController {

    private final RoutingStrategySelector selector;

    public RoutingStrategyController(RoutingStrategySelector selector) {
        this.selector = selector;
    }

    @GetMapping("/strategy")
    public Map<String, String> current() {
        return Map.of("strategy", selector.currentName());
    }

    @PatchMapping("/strategy")
    public Map<String, String> update(@Valid @RequestBody UpdateRoutingStrategyRequest request) {
        selector.select(request.strategy());
        return current();
    }
}