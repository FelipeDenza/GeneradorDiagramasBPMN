package com.desafiotecnicoimr.bpmn.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Component
public class ProcessValidator {

    static final int MAX_NODES = 30;
    static final int MAX_FLOWS = 60;
    static final int MAX_NAME_LENGTH = 120;
    private static final Pattern NODE_KEY = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,63}$");

    public void validate(ProcessDefinition process) {
        List<String> errors = new ArrayList<>();

        validateProcessName(process.processName(), errors);
        validateSize(process, errors);
        validateNodes(process.nodes(), errors);
        validateFlows(process.flows(), errors);

        Map<String, ProcessNode> nodesByKey = process.nodes().stream()
                .filter(Objects::nonNull)
                .filter(node -> hasText(node.key()))
                .collect(Collectors.toMap(
                        ProcessNode::key,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        validateReferencesAndNodeRules(process, nodesByKey, errors);
        validateReachability(process, nodesByKey, errors);

        if (!errors.isEmpty()) {
            throw new ProcessValidationException(errors);
        }
    }

    private void validateProcessName(String processName, List<String> errors) {
        if (!hasText(processName)) {
            errors.add("processName is required");
        } else if (processName.length() > MAX_NAME_LENGTH) {
            errors.add("processName must not exceed " + MAX_NAME_LENGTH + " characters");
        } else if (hasUnsupportedControlCharacter(processName)) {
            errors.add("processName contains unsupported control characters");
        }
    }

    private void validateSize(ProcessDefinition process, List<String> errors) {
        if (process.nodes().isEmpty()) {
            errors.add("At least one node is required");
        }
        if (process.nodes().size() > MAX_NODES) {
            errors.add("A process may contain at most " + MAX_NODES + " nodes");
        }
        if (process.flows().size() > MAX_FLOWS) {
            errors.add("A process may contain at most " + MAX_FLOWS + " flows");
        }
    }

    private void validateNodes(List<ProcessNode> nodes, List<String> errors) {
        Set<String> keys = new HashSet<>();
        long startEvents = 0;
        long endEvents = 0;

        for (int index = 0; index < nodes.size(); index++) {
            ProcessNode node = nodes.get(index);
            if (node == null) {
                errors.add("nodes[" + index + "] must not be null");
                continue;
            }
            if (!hasText(node.key())) {
                errors.add("nodes[" + index + "].key is required");
            } else if (!NODE_KEY.matcher(node.key()).matches()) {
                errors.add("nodes[" + index + "].key has an invalid format");
            } else if (!keys.add(node.key())) {
                errors.add("Node key '" + node.key() + "' is duplicated");
            }
            if (node.type() == null) {
                errors.add("nodes[" + index + "].type is required");
            } else if (node.type() == NodeType.START_EVENT) {
                startEvents++;
            } else if (node.type() == NodeType.END_EVENT) {
                endEvents++;
            }
            if (node.name() != null && node.name().length() > MAX_NAME_LENGTH) {
                errors.add("nodes[" + index + "].name must not exceed " + MAX_NAME_LENGTH + " characters");
            } else if (hasUnsupportedControlCharacter(node.name())) {
                errors.add("nodes[" + index + "].name contains unsupported control characters");
            }
        }

        if (startEvents != 1) {
            errors.add("The process must contain exactly one start event");
        }
        if (endEvents < 1) {
            errors.add("The process must contain at least one end event");
        }
    }

    private void validateFlows(List<ProcessFlow> flows, List<String> errors) {
        for (int index = 0; index < flows.size(); index++) {
            ProcessFlow flow = flows.get(index);
            if (flow == null) {
                errors.add("flows[" + index + "] must not be null");
                continue;
            }
            if (!hasText(flow.source())) {
                errors.add("flows[" + index + "].source is required");
            }
            if (!hasText(flow.target())) {
                errors.add("flows[" + index + "].target is required");
            }
            if (Objects.equals(flow.source(), flow.target()) && hasText(flow.source())) {
                errors.add("A flow may not connect node '" + flow.source() + "' to itself");
            }
            if (flow.name() != null && flow.name().length() > MAX_NAME_LENGTH) {
                errors.add("flows[" + index + "].name must not exceed " + MAX_NAME_LENGTH + " characters");
            } else if (hasUnsupportedControlCharacter(flow.name())) {
                errors.add("flows[" + index + "].name contains unsupported control characters");
            }
        }
    }

    private void validateReferencesAndNodeRules(
            ProcessDefinition process,
            Map<String, ProcessNode> nodesByKey,
            List<String> errors
    ) {
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Integer> outgoing = new HashMap<>();

        for (ProcessFlow flow : process.flows()) {
            if (flow == null || !hasText(flow.source()) || !hasText(flow.target())) {
                continue;
            }
            if (!nodesByKey.containsKey(flow.source())) {
                errors.add("Flow source '" + flow.source() + "' does not reference an existing node");
            } else {
                outgoing.merge(flow.source(), 1, Integer::sum);
            }
            if (!nodesByKey.containsKey(flow.target())) {
                errors.add("Flow target '" + flow.target() + "' does not reference an existing node");
            } else {
                incoming.merge(flow.target(), 1, Integer::sum);
            }
        }

        for (ProcessNode node : nodesByKey.values()) {
            if (node.type() == NodeType.START_EVENT && incoming.getOrDefault(node.key(), 0) > 0) {
                errors.add("Start event '" + node.key() + "' may not have incoming flows");
            }
            if (node.type() == NodeType.END_EVENT && outgoing.getOrDefault(node.key(), 0) > 0) {
                errors.add("End event '" + node.key() + "' may not have outgoing flows");
            }
        }
    }

    private void validateReachability(
            ProcessDefinition process,
            Map<String, ProcessNode> nodesByKey,
            List<String> errors
    ) {
        ProcessNode start = nodesByKey.values().stream()
                .filter(node -> node.type() == NodeType.START_EVENT)
                .findFirst()
                .orElse(null);
        if (start == null) {
            return;
        }

        Map<String, List<String>> adjacency = new HashMap<>();
        for (ProcessFlow flow : process.flows()) {
            if (flow != null && nodesByKey.containsKey(flow.source()) && nodesByKey.containsKey(flow.target())) {
                adjacency.computeIfAbsent(flow.source(), ignored -> new ArrayList<>()).add(flow.target());
            }
        }

        Set<String> visited = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        pending.add(start.key());
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (visited.add(current)) {
                pending.addAll(adjacency.getOrDefault(current, List.of()));
            }
        }

        nodesByKey.keySet().stream()
                .filter(key -> !visited.contains(key))
                .sorted()
                .forEach(key -> errors.add("Node '" + key + "' is not reachable from the start event"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasUnsupportedControlCharacter(String value) {
        if (value == null) {
            return false;
        }
        return value.chars().anyMatch(character -> Character.isISOControl(character)
                && character != '\n'
                && character != '\r'
                && character != '\t');
    }
}
