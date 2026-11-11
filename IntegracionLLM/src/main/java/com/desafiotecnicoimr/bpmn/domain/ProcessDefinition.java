package com.desafiotecnicoimr.bpmn.domain;

import java.util.List;

public record ProcessDefinition(
        String processName,
        List<ProcessNode> nodes,
        List<ProcessFlow> flows
) {
    public ProcessDefinition {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        flows = flows == null ? List.of() : List.copyOf(flows);
    }
}
