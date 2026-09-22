package com.zycus.hackthon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;

public interface AgentRepository extends JpaRepository<Agent, String> {

    List<Agent> findByStatusIn(List<AgentStatus> statuses);
}