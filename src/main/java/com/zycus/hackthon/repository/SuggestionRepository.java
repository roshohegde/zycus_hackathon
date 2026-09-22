package com.zycus.hackthon.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zycus.hackthon.domain.ReassignmentSuggestion;
import com.zycus.hackthon.domain.SuggestionStatus;
import com.zycus.hackthon.domain.TriggerReason;

public interface SuggestionRepository extends JpaRepository<ReassignmentSuggestion, Long> {

    Optional<ReassignmentSuggestion> findByOrderIdAndStatusAndTriggerReason(
            String orderId, SuggestionStatus status, TriggerReason triggerReason);
}