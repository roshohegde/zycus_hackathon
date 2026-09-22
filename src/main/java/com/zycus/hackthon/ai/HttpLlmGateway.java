package com.zycus.hackthon.ai;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

@Component
@ConditionalOnExpression("'${llm.provider:stub}' != 'stub'")
public class HttpLlmGateway implements LlmGateway {

    private final RestClient http;
    private final String provider;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public HttpLlmGateway(@Value("${llm.provider:stub}") String provider,
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.model:}") String model,
            @Value("${llm.base-url:}") String baseUrl) {
        this.http = RestClient.create();
        this.provider = provider.toLowerCase();
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    @Override
    public String call(String prompt) {
        return switch (provider) {
            case "gemini" -> callGemini(prompt);
            case "groq", "ollama" -> callOpenAiCompatible(prompt);
            default -> throw new IllegalStateException("Unsupported LLM provider: " + provider);
        };
    }

    private String callGemini(String prompt) {
        String url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        JsonNode response = http.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))))
                .retrieve().body(JsonNode.class);
        JsonNode text = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!text.isString()) {
            throw new IllegalStateException("Gemini response did not contain text");
        }
        return text.stringValue();
    }

    private String callOpenAiCompatible(String prompt) {
        String url = baseUrl + ("groq".equals(provider) ? "/openai/v1/chat/completions" : "/v1/chat/completions");
        JsonNode response = http.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", prompt))))
                .retrieve().body(JsonNode.class);
        JsonNode text = response.path("choices").path(0).path("message").path("content");
        if (!text.isString()) {
            throw new IllegalStateException("LLM response did not contain message content");
        }
        return text.stringValue();
    }
}