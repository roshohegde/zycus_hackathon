package com.zycus.hackthon.routing;

import com.zycus.hackthon.domain.TriggerReason;

public record TriggerContext(TriggerReason reason, String failedAgentId, String excludedAgentId) {

    public TriggerContext(TriggerReason reason, String failedAgentId) {
        this(reason, failedAgentId, null);
    }

    public static TriggerContext initial() {
        return new TriggerContext(TriggerReason.INITIAL, null, null);
    }
}