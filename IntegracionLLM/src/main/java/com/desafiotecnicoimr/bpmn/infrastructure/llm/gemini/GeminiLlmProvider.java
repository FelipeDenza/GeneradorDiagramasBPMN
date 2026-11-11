package com.desafiotecnicoimr.bpmn.infrastructure.llm.gemini;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmProvider;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmRequest;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.ProviderDescriptor;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmProviderException;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.support.ProviderHttpErrors;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.support.ProviderSchemaAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class GeminiLlmProvider implements LlmProvider {

    private final ProviderDescriptor descriptor;
    private final String apiKey;
    private final RestClient.Builder clientBuilder;
    private final ObjectMapper objectMapper;
    private volatile RestClient client;

    public GeminiLlmProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${llm.gemini.api-key:}") String apiKey,
            @Value("${llm.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${llm.gemini.model:gemini-3.8-flash}") String model
    ) {
        this.descriptor = new ProviderDescriptor("gemini", "Google Gemini", model);
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey);
    }

    @Override
    public ProviderDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generate(LlmRequest request) {
        try {
            JsonNode schema = ProviderSchemaAdapter.geminiSchema(objectMapper, request.responseSchema());
            Map<String, Object> body = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", request.systemInstruction()))
                    ),
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", request.userMessage()))
                    )),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "responseJsonSchema", schema
                    )
            );

            JsonNode response = client().post()
                    .uri("/models/{model}:generateContent", descriptor.model())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return extractContent(response);
        } catch (RestClientResponseException exception) {
            throw ProviderHttpErrors.translate(descriptor.id(), exception);
        } catch (RestClientException exception) {
            throw ProviderHttpErrors.translate(descriptor.id(), exception);
        } catch (JacksonException exception) {
            throw new LlmProviderException(
                    descriptor.id(),
                    LlmProviderException.Failure.INVALID_RESPONSE,
                    exception
            );
        }
    }

    private RestClient client() {
        RestClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (client == null) {
                client = clientBuilder.build();
            }
            return client;
        }
    }

    private String extractContent(JsonNode response) {
        if (response == null) {
            throw new LlmProviderException(descriptor.id(), LlmProviderException.Failure.INVALID_RESPONSE);
        }

        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.size() == 0) {
            String blockReason = response.path("promptFeedback").path("blockReason").asText("");
            LlmProviderException.Failure failure = blockReason.isBlank()
                    ? LlmProviderException.Failure.INVALID_RESPONSE
                    : LlmProviderException.Failure.REJECTED;
            throw new LlmProviderException(descriptor.id(), failure);
        }

        StringBuilder content = new StringBuilder();
        candidates.path(0).path("content").path("parts").forEach(part -> {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                content.append(text);
            }
        });
        if (content.isEmpty()) {
            throw new LlmProviderException(descriptor.id(), LlmProviderException.Failure.INVALID_RESPONSE);
        }
        return content.toString();
    }
}
