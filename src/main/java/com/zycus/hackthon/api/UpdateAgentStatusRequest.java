package com.zycus.hackthon.api;

import com.zycus.hackthon.domain.AgentStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateAgentStatusRequest(@NotNull AgentStatus status) {
}