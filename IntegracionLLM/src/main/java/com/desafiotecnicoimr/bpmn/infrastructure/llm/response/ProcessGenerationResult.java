package com.desafiotecnicoimr.bpmn.infrastructure.llm.response;

import com.desafiotecnicoimr.bpmn.domain.ProcessDefinition;

public record ProcessGenerationResult(
        Boolean isProcessDescription,
        String reason,
        ProcessDefinition process
) {
}
