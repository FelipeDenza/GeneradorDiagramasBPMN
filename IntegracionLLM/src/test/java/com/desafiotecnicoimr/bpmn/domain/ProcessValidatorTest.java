package com.desafiotecnicoimr.bpmn.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessValidatorTest {

    private final ProcessValidator validator = new ProcessValidator();

    @Test
    void acceptsAConnectedProcess() {
        ProcessDefinition process = new ProcessDefinition(
                "Purchase approval",
                List.of(
                        new ProcessNode("start", NodeType.START_EVENT, "Request received"),
                        new ProcessNode("review", NodeType.TASK, "Review request"),
                        new ProcessNode("end", NodeType.END_EVENT, "Completed")
                ),
                List.of(
                        new ProcessFlow("start", "review", null),
                        new ProcessFlow("review", "end", null)
                )
        );

        assertThatCode(() -> validator.validate(process)).doesNotThrowAnyException();
    }

    @Test
    void reportsNodesThatCannotBeReachedFromTheStart() {
        ProcessDefinition process = new ProcessDefinition(
                "Broken process",
                List.of(
                        new ProcessNode("start", NodeType.START_EVENT, "Start"),
                        new ProcessNode("orphan", NodeType.TASK, "Orphan task"),
                        new ProcessNode("end", NodeType.END_EVENT, "End")
                ),
                List.of(new ProcessFlow("start", "end", null))
        );

        assertThatThrownBy(() -> validator.validate(process))
                .isInstanceOf(ProcessValidationException.class)
                .satisfies(exception -> assertThat(((ProcessValidationException) exception).getErrors())
                        .contains("Node 'orphan' is not reachable from the start event"));
    }

    @Test
    void enforcesNodeLimitEvenWhenProviderSchemaOmitsIt() {
        assertThatCode(() -> validator.validate(linearProcess(30))).doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validate(linearProcess(31)))
                .isInstanceOf(ProcessValidationException.class)
                .satisfies(exception -> assertThat(((ProcessValidationException) exception).getErrors())
                        .containsExactly("A process may contain at most 30 nodes"));
    }

    @Test
    void enforcesFlowLimitEvenWhenProviderSchemaOmitsIt() {
        ProcessDefinition base = linearProcess(2);
        ProcessFlow flow = base.flows().getFirst();
        assertThatCode(() -> validator.validate(new ProcessDefinition(
                base.processName(), base.nodes(), java.util.Collections.nCopies(60, flow)
        ))).doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validate(new ProcessDefinition(
                base.processName(), base.nodes(), java.util.Collections.nCopies(61, flow)
        )))
                .isInstanceOf(ProcessValidationException.class)
                .satisfies(exception -> assertThat(((ProcessValidationException) exception).getErrors())
                        .containsExactly("A process may contain at most 60 flows"));
    }

    private ProcessDefinition linearProcess(int nodeCount) {
        List<ProcessNode> nodes = IntStream.range(0, nodeCount)
                .mapToObj(index -> new ProcessNode("node" + index,
                        index == 0 ? NodeType.START_EVENT
                                : index == nodeCount - 1 ? NodeType.END_EVENT : NodeType.TASK,
                        null))
                .toList();
        List<ProcessFlow> flows = IntStream.range(0, nodeCount - 1)
                .mapToObj(index -> new ProcessFlow("node" + index, "node" + (index + 1), null))
                .toList();
        return new ProcessDefinition("Node limit test", nodes, flows);
    }
}
