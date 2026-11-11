package com.desafiotecnicoimr.bpmn.infrastructure.llm;

public interface LlmProvider {

    ProviderDescriptor descriptor();

    String generate(LlmRequest request);

    default boolean isConfigured() {
        return true;
    }
}
