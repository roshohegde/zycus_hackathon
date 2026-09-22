package com.zycus.hackthon.api;

import java.time.Instant;

import com.zycus.hackthon.domain.Order;
import com.zycus.hackthon.domain.OrderStatus;

public record OrderResponse(String id, String description, String assignedAgentId,
        OrderStatus status, Instant createdAt, String pickupZone, String dropoffZone) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getDescription(),
                order.getAssignedAgent().getId(), order.getStatus(), order.getCreatedAt(),
                order.getPickupZone(), order.getDropoffZone());
    }
}