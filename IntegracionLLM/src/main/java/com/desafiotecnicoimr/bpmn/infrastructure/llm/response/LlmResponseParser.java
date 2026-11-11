package com.desafiotecnicoimr.bpmn.infrastructure.llm.response;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmResponseInvalidException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

@Component
public class LlmResponseParser {

    private final ObjectMapper strictObjectMapper;

    public LlmResponseParser(ObjectMapper objectMapper) {
        this.strictObjectMapper = objectMapper.rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public ProcessGenerationResult parse(String response) {
        if (response == null || response.isBlank()) {
            throw new LlmResponseInvalidException("The response is empty", null);
        }
        try {
            return strictObjectMapper.readValue(response, ProcessGenerationResult.class);
        } catch (JacksonException exception) {
            throw new LlmResponseInvalidException("The response is not valid process JSON", exception);
        }
    }
}
