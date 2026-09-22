package com.zycus.hackthon.event;

public record ReassignmentRejectedEvent(String orderId, String failedAgentId, String rejectedAgentId) {
}