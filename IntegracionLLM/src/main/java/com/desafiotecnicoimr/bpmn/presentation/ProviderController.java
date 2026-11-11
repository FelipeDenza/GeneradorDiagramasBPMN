package com.desafiotecnicoimr.bpmn.presentation;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmProviderRegistry;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.ProviderDescriptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private final LlmProviderRegistry providerRegistry;

    public ProviderController(LlmProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @GetMapping
    public List<ProviderDescriptor> availableProviders() {
        return providerRegistry.availableProviders();
    }
}
