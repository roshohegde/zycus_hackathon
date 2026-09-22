package com.zycus.hackthon.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zycus.hackthon.domain.Agent;
import com.zycus.hackthon.domain.AgentStatus;
import com.zycus.hackthon.domain.Order;
import com.zycus.hackthon.repository.AgentRepository;
import com.zycus.hackthon.repository.OrderRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(AgentRepository agentRepository, OrderRepository orderRepository) {
        return args -> {
            if (agentRepository.count() > 0) {
                return;
            }

            var agents = agentRepository.saveAll(List.of(
                    new Agent("AGT-001", "Priya Sharma", AgentStatus.BUSY, 3, "Koramangala", 5),
                    new Agent("AGT-002", "Rahul Verma", AgentStatus.AVAILABLE, 0, "HSR Layout", 4),
                    new Agent("AGT-003", "Ananya Iyer", AgentStatus.BUSY, 2, "Whitefield", 4),
                    new Agent("AGT-004", "Kiran Nair", AgentStatus.AVAILABLE, 0, "MG Road", 5),
                    new Agent("AGT-005", "Deepak Mehta", AgentStatus.BUSY, 3, "Bellandur", 4)));

            var first = agents.get(0);
            var third = agents.get(2);
            var fifth = agents.get(4);
            orderRepository.saveAll(List.of(
                    new Order("ORD-001", "Electronics - Koramangala to Indiranagar", first, "Koramangala", "Indiranagar"),
                    new Order("ORD-002", "Groceries - HSR Layout to BTM", first, "HSR Layout", "BTM"),
                    new Order("ORD-003", "Pharma - Whitefield to Marathahalli", third, "Whitefield", "Marathahalli"),
                    new Order("ORD-004", "Documents - MG Road to Jayanagar", fifth, "MG Road", "Jayanagar"),
                    new Order("ORD-005", "Food - Bellandur to Electronic City", fifth, "Bellandur", "Electronic City"),
                    new Order("ORD-006", "Apparel - Malleshwaram to Rajajinagar", fifth, "Malleshwaram", "Rajajinagar"),
                    new Order("ORD-007", "Books - Banashankari to JP Nagar", third, "Banashankari", "JP Nagar"),
                    new Order("ORD-008", "Hardware - Peenya to Yeshwanthpur", first, "Peenya", "Yeshwanthpur")));
        };
    }
}