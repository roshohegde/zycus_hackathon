package com.zycus.hackthon.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    private String description;

    @ManyToOne(optional = false)
    private Agent assignedAgent;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;

    private String pickupZone;

    private String dropoffZone;

    protected Order() {
    }

    public Order(String id, String description, Agent assignedAgent) {
        this.id = id;
        this.description = description;
        this.assignedAgent = assignedAgent;
        this.status = OrderStatus.ASSIGNED;
        this.createdAt = Instant.now();
    }

    public Order(String id, String description, Agent assignedAgent, String pickupZone, String dropoffZone) {
        this(id, description, assignedAgent);
        this.pickupZone = pickupZone;
        this.dropoffZone = dropoffZone;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Agent getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(Agent assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPickupZone() {
        return pickupZone;
    }

    public String getDropoffZone() {
        return dropoffZone;
    }
}