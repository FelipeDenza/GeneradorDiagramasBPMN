package com.desafiotecnicoimr.bpmn.application;

import java.util.List;

public record GeneratedDiagram(
        String bpmnXml,
        String provider,
        String model,
        List<String> warnings
) {
}
