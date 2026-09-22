package com.zycus.hackthon.api;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String description,
        @NotBlank String agentId) {
}