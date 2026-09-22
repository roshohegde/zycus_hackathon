package com.zycus.hackthon.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoutingStrategyRequest(@NotBlank String strategy) {
}