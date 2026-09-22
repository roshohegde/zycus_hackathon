package com.zycus.hackthon.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zycus.hackthon.domain.SuggestionStatus;
import com.zycus.hackthon.service.SuggestionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public List<SuggestionResponse> find(@RequestParam(required = false) SuggestionStatus status) {
        return suggestionService.find(status).stream().map(SuggestionResponse::from).toList();
    }

    @PatchMapping("/{id}")
    public SuggestionResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateSuggestionRequest request) {
        return SuggestionResponse.from(suggestionService.update(id, request));
    }
}