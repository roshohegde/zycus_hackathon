package com.zycus.hackthon.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agents")
public class Agent {

    @Id
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private AgentStatus status;

    private int activeOrderCount;

    private String currentZone;

    private Integer maxCapacity;

    protected Agent() {
    }

    public Agent(String id, String name, AgentStatus status, int activeOrderCount) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.activeOrderCount = activeOrderCount;
    }

    public Agent(String id, String name, AgentStatus status, int activeOrderCount,
            String currentZone, Integer maxCapacity) {
        this(id, name, status, activeOrderCount);
        this.currentZone = currentZone;
        this.maxCapacity = maxCapacity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public int getActiveOrderCount() {
        return activeOrderCount;
    }

    public void setActiveOrderCount(int activeOrderCount) {
        this.activeOrderCount = activeOrderCount;
    }

    public String getCurrentZone() {
        return currentZone;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }
}