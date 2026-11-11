package com.desafiotecnicoimr.bpmn.infrastructure.llm.openai.openrouter;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.ProviderDescriptor;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.openai.OpenAiCompatibleLlmProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class OpenRouterLlmProvider extends OpenAiCompatibleLlmProvider {

    public OpenRouterLlmProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${llm.openrouter.api-key:}") String apiKey,
            @Value("${llm.openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${llm.openrouter.model:openrouter/free}") String model
    ) {
        super(
                builder,
                objectMapper,
                new ProviderDescriptor("openrouter", "OpenRouter", model),
                apiKey,
                baseUrl,
                false,
                Map.of("X-Title", "BPMN Prompt")
        );
    }
}
