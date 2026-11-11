package com.desafiotecnicoimr.bpmn.infrastructure.llm;

public record LlmRequest(
        String systemInstruction,
        String userMessage,
        String responseSchema
) {
}
