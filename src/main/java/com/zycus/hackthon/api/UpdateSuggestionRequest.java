package com.zycus.hackthon.api;

import com.zycus.hackthon.domain.SuggestionStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateSuggestionRequest(@NotNull SuggestionStatus status) {
}