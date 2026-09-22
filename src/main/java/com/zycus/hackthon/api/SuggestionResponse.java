package com.zycus.hackthon.api;

import java.time.Instant;

import com.zycus.hackthon.domain.ReassignmentSuggestion;
import com.zycus.hackthon.domain.SuggestionStatus;
import com.zycus.hackthon.domain.TriggerReason;

public record SuggestionResponse(Long id, String orderId, String recommendedAgentId,
        double confidence, String reasoning, SuggestionStatus status,
        TriggerReason triggerReason, Instant createdAt) {

    public static SuggestionResponse from(ReassignmentSuggestion suggestion) {
        return new SuggestionResponse(suggestion.getId(), suggestion.getOrder().getId(),
                suggestion.getRecommendedAgent().getId(), suggestion.getConfidence(),
                suggestion.getReasoning(), suggestion.getStatus(), suggestion.getTriggerReason(),
                suggestion.getCreatedAt());
    }
}