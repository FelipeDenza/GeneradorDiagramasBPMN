package com.desafiotecnicoimr.bpmn.application;

import com.desafiotecnicoimr.bpmn.infrastructure.bpmn.BpmnXmlGenerator;
import com.desafiotecnicoimr.bpmn.domain.ProcessDefinition;
import com.desafiotecnicoimr.bpmn.domain.ProcessValidationException;
import com.desafiotecnicoimr.bpmn.domain.ProcessValidator;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmProvider;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmProviderRegistry;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmRequest;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmResponseInvalidException;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.response.LlmResponseParser;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.prompt.PromptFactory;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.response.ProcessGenerationResult;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.ProviderDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateDiagramUseCase {

    private final PromptPolicy promptPolicy;
    private final LlmProviderRegistry providerRegistry;
    private final PromptFactory promptFactory;
    private final LlmResponseParser responseParser;
    private final ProcessValidator processValidator;
    private final BpmnXmlGenerator xmlGenerator;

    public GenerateDiagramUseCase(
            PromptPolicy promptPolicy,
            LlmProviderRegistry providerRegistry,
            PromptFactory promptFactory,
            LlmResponseParser responseParser,
            ProcessValidator processValidator,
            BpmnXmlGenerator xmlGenerator
    ) {
        this.promptPolicy = promptPolicy;
        this.providerRegistry = providerRegistry;
        this.promptFactory = promptFactory;
        this.responseParser = responseParser;
        this.processValidator = processValidator;
        this.xmlGenerator = xmlGenerator;
    }

    public GeneratedDiagram generate(String prompt, String providerId) {
        promptPolicy.validate(prompt);
        LlmProvider provider = providerRegistry.get(providerId);

        String firstResponse = provider.generate(promptFactory.initial(prompt));
        ProcessDefinition process;
        try {
            process = parseAndValidate(firstResponse);
        } catch (LlmResponseInvalidException | ProcessValidationException firstFailure) {
            process = repairOnce(provider, prompt, firstResponse, errorsFrom(firstFailure));
        }

        ProviderDescriptor descriptor = provider.descriptor();
        return new GeneratedDiagram(
                xmlGenerator.generate(process),
                descriptor.id(),
                descriptor.model(),
                List.of()
        );
    }

    private ProcessDefinition repairOnce(
            LlmProvider provider,
            String prompt,
            String invalidResponse,
            List<String> validationErrors
    ) {
        LlmRequest repairRequest = promptFactory.repair(prompt, invalidResponse, validationErrors);
        String repairedResponse = provider.generate(repairRequest);
        try {
            return parseAndValidate(repairedResponse);
        } catch (LlmResponseInvalidException | ProcessValidationException secondFailure) {
            throw new GeneratedProcessInvalidException(secondFailure);
        }
    }

    private ProcessDefinition parseAndValidate(String response) {
        ProcessGenerationResult result = responseParser.parse(response);
        if (result.isProcessDescription() == null) {
            throw new LlmResponseInvalidException("The process classification is missing", null);
        }
        if (!result.isProcessDescription()) {
            throw new NotProcessDescriptionException();
        }
        ProcessDefinition process = result.process();
        if (process == null) {
            throw new LlmResponseInvalidException("The classified process is missing", null);
        }
        processValidator.validate(process);
        return process;
    }

    private List<String> errorsFrom(RuntimeException failure) {
        if (failure instanceof LlmResponseInvalidException invalidResponse) {
            return invalidResponse.getErrors();
        }
        return ((ProcessValidationException) failure).getErrors();
    }
}
