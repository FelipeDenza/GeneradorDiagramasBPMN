package com.desafiotecnicoimr.bpmn.infrastructure.llm.openai.groq;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.ProviderDescriptor;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.openai.OpenAiCompatibleLlmProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class GroqLlmProvider extends OpenAiCompatibleLlmProvider {

    public GroqLlmProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${llm.groq.api-key:}") String apiKey,
            @Value("${llm.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${llm.groq.model:openai/gpt-oss-20b}") String model
    ) {
        super(
                builder,
                objectMapper,
                new ProviderDescriptor("groq", "Groq", model),
                apiKey,
                baseUrl,
                true,
                Map.of()
        );
    }
}
