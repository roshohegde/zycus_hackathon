package com.zycus.hackthon.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.zycus.hackthon.domain.OrderStatus;
import com.zycus.hackthon.service.OrderService;
import com.zycus.hackthon.service.SuggestionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final SuggestionService suggestionService;

    public OrderController(OrderService orderService, SuggestionService suggestionService) {
        this.orderService = orderService;
        this.suggestionService = suggestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(orderService.create(request));
    }

    @GetMapping
    public List<OrderResponse> find(@RequestParam(required = false) OrderStatus status) {
        return orderService.find(status).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/{id}/suggest")
    @ResponseStatus(HttpStatus.CREATED)
    public SuggestionResponse suggest(@PathVariable String id) {
        return SuggestionResponse.from(suggestionService.suggest(id,
                com.zycus.hackthon.routing.TriggerContext.initial()));
    }
}