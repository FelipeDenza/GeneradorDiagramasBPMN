package com.desafiotecnicoimr.bpmn.application;

import com.desafiotecnicoimr.bpmn.infrastructure.bpmn.BpmnXmlGenerator;
import com.desafiotecnicoimr.bpmn.domain.ProcessValidator;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmProvider;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmProviderRegistry;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.LlmRequest;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.response.LlmResponseParser;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.prompt.ProcessSchema;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.prompt.PromptFactory;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.ProviderDescriptor;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateDiagramUseCaseTest {

    @Test
    void repairsOneInvalidResponseBeforeGeneratingXml() {
        FakeProvider provider = new FakeProvider(
                "{\"isProcessDescription\":true,\"reason\":null,\"process\":{\"processName\":\"Invalid\",\"nodes\":[],\"flows\":[]}}",
                """
                        {
                          "isProcessDescription": true,
                          "reason": null,
                          "process": {
                            "processName": "Vacation approval",
                            "nodes": [
                              {"key": "start", "type": "START_EVENT", "name": "Request received"},
                              {"key": "approve", "type": "TASK", "name": "Approve request"},
                              {"key": "end", "type": "END_EVENT", "name": "Finished"}
                            ],
                            "flows": [
                              {"source": "start", "target": "approve"},
                              {"source": "approve", "target": "end"}
                            ]
                          }
                        }
                        """
        );
        ProcessSchema schema = new ProcessSchema();
        GenerateDiagramUseCase useCase = new GenerateDiagramUseCase(
                new PromptPolicy(),
                new LlmProviderRegistry(List.of(provider)),
                new PromptFactory(schema),
                new LlmResponseParser(new ObjectMapper()),
                new ProcessValidator(),
                new BpmnXmlGenerator()
        );

        GeneratedDiagram result = useCase.generate("Vacation approval process", "fake");

        assertThat(provider.calls).isEqualTo(2);
        assertThat(result.provider()).isEqualTo("fake");
        assertThat(result.bpmnXml()).contains("Vacation approval", "Task_1", "Flow_2");
    }

    @Test
    void rejectsTextThatTheProviderClassifiesAsNotAProcess() {
        FakeProvider provider = new FakeProvider(
                "{\"isProcessDescription\":false,\"reason\":\"It only contains unrelated facts\",\"process\":null}"
        );
        ProcessSchema schema = new ProcessSchema();
        GenerateDiagramUseCase useCase = new GenerateDiagramUseCase(
                new PromptPolicy(),
                new LlmProviderRegistry(List.of(provider)),
                new PromptFactory(schema),
                new LlmResponseParser(new ObjectMapper()),
                new ProcessValidator(),
                new BpmnXmlGenerator()
        );

        assertThatThrownBy(() -> useCase.generate("Penguins are birds. My favorite color is blue.", "fake"))
                .isInstanceOf(NotProcessDescriptionException.class);
        assertThat(provider.calls).isEqualTo(1);
    }

    private static class FakeProvider implements LlmProvider {

        private final Queue<String> responses;
        private int calls;

        private FakeProvider(String... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public ProviderDescriptor descriptor() {
            return new ProviderDescriptor("fake", "Fake provider", "fake-model");
        }

        @Override
        public String generate(LlmRequest request) {
            calls++;
            return responses.remove();
        }
    }
}
