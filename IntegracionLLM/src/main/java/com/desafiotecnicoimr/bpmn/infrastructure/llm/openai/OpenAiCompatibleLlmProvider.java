package com.desafiotecnicoimr.bpmn.infrastructure.llm.openai;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmProvider;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmRequest;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.ProviderDescriptor;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmProviderException;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.support.ProviderHttpErrors;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.support.ProviderSchemaAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class OpenAiCompatibleLlmProvider implements LlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCompatibleLlmProvider.class);

    private final ProviderDescriptor descriptor;
    private final String apiKey;
    private final boolean strictSchema;
    private final RestClient.Builder clientBuilder;
    private final ObjectMapper objectMapper;
    private volatile RestClient client;

    protected OpenAiCompatibleLlmProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            ProviderDescriptor descriptor,
            String apiKey,
            String baseUrl,
            boolean strictSchema,
            Map<String, String> additionalHeaders
    ) {
        this.descriptor = descriptor;
        this.apiKey = apiKey;
        this.strictSchema = strictSchema;
        this.objectMapper = objectMapper;

        this.clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        additionalHeaders.forEach(this.clientBuilder::defaultHeader);
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
            JsonNode schema = ProviderSchemaAdapter.portableSchema(objectMapper, request.responseSchema());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", descriptor.model());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", request.systemInstruction()),
                    Map.of("role", "user", "content", request.userMessage())
            ));
            body.put("temperature", 0.1);
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "bpmn_process",
                            "strict", strictSchema,
                            "schema", schema
                    )
            ));
            customizeRequest(body);

            JsonNode response = client().post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return extractContent(response);
        } catch (RestClientResponseException exception) {
            logProviderError(exception);
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

    protected void customizeRequest(Map<String, Object> body) {
    }

    private void logProviderError(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body.length() > 400) {
            body = body.substring(0, 400);
        }
        logger.warn("{} API returned HTTP {}: {}",
                descriptor.id(), exception.getStatusCode().value(), body);
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

        JsonNode message = response.path("choices").path(0).path("message");
        String refusal = message.path("refusal").asText("");
        if (!refusal.isBlank()) {
            throw new LlmProviderException(descriptor.id(), LlmProviderException.Failure.REJECTED);
        }

        String content = message.path("content").asText("");
        if (content.isBlank()) {
            throw new LlmProviderException(descriptor.id(), LlmProviderException.Failure.INVALID_RESPONSE);
        }
        return content;
    }
}
