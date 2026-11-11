package com.desafiotecnicoimr.bpmn.infrastructure.llm;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.UnknownProviderException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LlmProviderRegistry {

    private final Map<String, LlmProvider> providers;

    public LlmProviderRegistry(List<LlmProvider> providers) {
        Map<String, LlmProvider> indexedProviders = new LinkedHashMap<>();
        providers.stream()
                .filter(LlmProvider::isConfigured)
                .sorted(Comparator.comparing(provider -> provider.descriptor().id()))
                .forEach(provider -> indexedProviders.put(normalize(provider.descriptor().id()), provider));
        this.providers = Map.copyOf(indexedProviders);
    }

    public LlmProvider get(String providerId) {
        LlmProvider provider = providers.get(normalize(providerId));
        if (provider == null) {
            throw new UnknownProviderException(providerId);
        }
        return provider;
    }

    public List<ProviderDescriptor> availableProviders() {
        return providers.values().stream().map(LlmProvider::descriptor).toList();
    }

    private String normalize(String providerId) {
        return providerId == null ? "" : providerId.trim().toLowerCase(Locale.ROOT);
    }
}
