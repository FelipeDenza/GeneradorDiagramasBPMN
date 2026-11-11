package com.desafiotecnicoimr.bpmn.infrastructure.bpmn;

import com.desafiotecnicoimr.bpmn.domain.NodeType;
import com.desafiotecnicoimr.bpmn.domain.ProcessDefinition;
import com.desafiotecnicoimr.bpmn.domain.ProcessFlow;
import com.desafiotecnicoimr.bpmn.domain.ProcessNode;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BpmnXmlGenerator {

    private static final String BPMN_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String TARGET_NAMESPACE = "https://desafiotecnicoimr.com/bpmn";

    public String generate(ProcessDefinition process) {
        try {
            StringWriter output = new StringWriter();
            XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            writeDocument(writer, process);
            writer.close();
            return output.toString();
        } catch (XMLStreamException exception) {
            throw new BpmnGenerationException("Could not generate BPMN XML", exception);
        }
    }

    private void writeDocument(XMLStreamWriter writer, ProcessDefinition process) throws XMLStreamException {
        Map<String, String> nodeIds = createNodeIds(process);
        List<IndexedFlow> indexedFlows = createFlowIds(process);
        Map<String, List<String>> incomingFlows = indexFlowsByTarget(indexedFlows);
        Map<String, List<String>> outgoingFlows = indexFlowsBySource(indexedFlows);

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("bpmn", "definitions", BPMN_NAMESPACE);
        writer.writeNamespace("bpmn", BPMN_NAMESPACE);
        writer.writeNamespace("xsi", XSI_NAMESPACE);
        writer.writeAttribute("id", "Definitions_1");
        writer.writeAttribute("targetNamespace", TARGET_NAMESPACE);

        writer.writeStartElement("bpmn", "process", BPMN_NAMESPACE);
        writer.writeAttribute("id", "Process_1");
        writer.writeAttribute("name", process.processName());
        writer.writeAttribute("isExecutable", "false");

        for (ProcessNode node : process.nodes()) {
            writeNode(
                    writer,
                    node,
                    nodeIds.get(node.key()),
                    incomingFlows.getOrDefault(node.key(), List.of()),
                    outgoingFlows.getOrDefault(node.key(), List.of())
            );
        }
        for (IndexedFlow indexedFlow : indexedFlows) {
            writeFlow(writer, indexedFlow.flow(), indexedFlow.id(), nodeIds);
        }

        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
    }

    private Map<String, String> createNodeIds(ProcessDefinition process) {
        Map<NodeType, Integer> counters = new EnumMap<>(NodeType.class);
        Map<String, String> ids = new LinkedHashMap<>();
        for (ProcessNode node : process.nodes()) {
            int index = counters.merge(node.type(), 1, Integer::sum);
            ids.put(node.key(), idPrefix(node.type()) + "_" + index);
        }
        return ids;
    }

    private List<IndexedFlow> createFlowIds(ProcessDefinition process) {
        List<IndexedFlow> indexedFlows = new ArrayList<>();
        for (int index = 0; index < process.flows().size(); index++) {
            indexedFlows.add(new IndexedFlow("Flow_" + (index + 1), process.flows().get(index)));
        }
        return List.copyOf(indexedFlows);
    }

    private Map<String, List<String>> indexFlowsBySource(List<IndexedFlow> flows) {
        Map<String, List<String>> indexed = new HashMap<>();
        for (IndexedFlow flow : flows) {
            indexed.computeIfAbsent(flow.flow().source(), ignored -> new ArrayList<>()).add(flow.id());
        }
        return indexed;
    }

    private Map<String, List<String>> indexFlowsByTarget(List<IndexedFlow> flows) {
        Map<String, List<String>> indexed = new HashMap<>();
        for (IndexedFlow flow : flows) {
            indexed.computeIfAbsent(flow.flow().target(), ignored -> new ArrayList<>()).add(flow.id());
        }
        return indexed;
    }

    private String idPrefix(NodeType type) {
        return switch (type) {
            case START_EVENT -> "StartEvent";
            case END_EVENT -> "EndEvent";
            case TASK -> "Task";
            case EXCLUSIVE_GATEWAY -> "ExclusiveGateway";
            case PARALLEL_GATEWAY -> "ParallelGateway";
        };
    }

    private void writeNode(
            XMLStreamWriter writer,
            ProcessNode node,
            String id,
            List<String> incomingFlows,
            List<String> outgoingFlows
    ) throws XMLStreamException {
        String elementName = switch (node.type()) {
            case START_EVENT -> "startEvent";
            case END_EVENT -> "endEvent";
            case TASK -> "task";
            case EXCLUSIVE_GATEWAY -> "exclusiveGateway";
            case PARALLEL_GATEWAY -> "parallelGateway";
        };
        writer.writeStartElement("bpmn", elementName, BPMN_NAMESPACE);
        writer.writeAttribute("id", id);
        writeOptionalName(writer, node.name());
        writeFlowReferences(writer, "incoming", incomingFlows);
        writeFlowReferences(writer, "outgoing", outgoingFlows);
        writer.writeEndElement();
    }

    private void writeFlowReferences(
            XMLStreamWriter writer,
            String elementName,
            List<String> flowIds
    ) throws XMLStreamException {
        for (String flowId : flowIds) {
            writer.writeStartElement("bpmn", elementName, BPMN_NAMESPACE);
            writer.writeCharacters(flowId);
            writer.writeEndElement();
        }
    }

    private void writeFlow(
            XMLStreamWriter writer,
            ProcessFlow flow,
            String id,
            Map<String, String> nodeIds
    ) throws XMLStreamException {
        writer.writeStartElement("bpmn", "sequenceFlow", BPMN_NAMESPACE);
        writer.writeAttribute("id", id);
        writer.writeAttribute("sourceRef", nodeIds.get(flow.source()));
        writer.writeAttribute("targetRef", nodeIds.get(flow.target()));
        writeOptionalName(writer, flow.name());
        writer.writeEndElement();
    }

    private void writeOptionalName(XMLStreamWriter writer, String name) throws XMLStreamException {
        if (name != null && !name.isBlank()) {
            writer.writeAttribute("name", name);
        }
    }

    private record IndexedFlow(String id, ProcessFlow flow) {
    }
}
