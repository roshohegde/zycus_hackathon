package com.zycus.hackthon.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reassignment_suggestions")
public class ReassignmentSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Order order;

    @ManyToOne(optional = false)
    private Agent recommendedAgent;

    private double confidence;

    private String reasoning;

    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    private TriggerReason triggerReason;

    private Instant createdAt;

    protected ReassignmentSuggestion() {
    }

    public ReassignmentSuggestion(Order order, Agent recommendedAgent, double confidence,
            String reasoning, TriggerReason triggerReason) {
        this.order = order;
        this.recommendedAgent = recommendedAgent;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.status = SuggestionStatus.PENDING;
        this.triggerReason = triggerReason;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Agent getRecommendedAgent() {
        return recommendedAgent;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }

    public TriggerReason getTriggerReason() {
        return triggerReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}