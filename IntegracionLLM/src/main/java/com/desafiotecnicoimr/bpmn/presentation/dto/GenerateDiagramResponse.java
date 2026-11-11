package com.desafiotecnicoimr.bpmn.presentation.dto;

import com.desafiotecnicoimr.bpmn.application.GeneratedDiagram;

import java.util.List;

public record GenerateDiagramResponse(
        String bpmnXml,
        String provider,
        String model,
        List<String> warnings
) {
    public static GenerateDiagramResponse from(GeneratedDiagram diagram) {
        return new GenerateDiagramResponse(
                diagram.bpmnXml(),
                diagram.provider(),
                diagram.model(),
                diagram.warnings()
        );
    }
}
