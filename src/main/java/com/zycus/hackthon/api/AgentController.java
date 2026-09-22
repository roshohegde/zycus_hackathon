package com.zycus.hackthon.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zycus.hackthon.service.AgentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public List<AgentResponse> findAll() {
        return agentService.findAll().stream().map(AgentResponse::from).toList();
    }

    @PatchMapping("/{id}/status")
    public AgentResponse updateStatus(@PathVariable String id,
            @Valid @RequestBody UpdateAgentStatusRequest request) {
        return AgentResponse.from(agentService.updateStatus(id, request));
    }
}