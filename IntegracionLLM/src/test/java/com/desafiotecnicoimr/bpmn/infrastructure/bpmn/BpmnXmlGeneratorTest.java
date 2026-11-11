package com.desafiotecnicoimr.bpmn.infrastructure.bpmn;

import com.desafiotecnicoimr.bpmn.domain.NodeType;
import com.desafiotecnicoimr.bpmn.domain.ProcessDefinition;
import com.desafiotecnicoimr.bpmn.domain.ProcessFlow;
import com.desafiotecnicoimr.bpmn.domain.ProcessNode;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BpmnXmlGeneratorTest {

    private final BpmnXmlGenerator generator = new BpmnXmlGenerator();

    @Test
    void generatesParseableBpmnAndEscapesUntrustedNames() throws Exception {
        ProcessDefinition process = new ProcessDefinition(
                "Orders & returns",
                List.of(
                        new ProcessNode("start", NodeType.START_EVENT, "Order <received>"),
                        new ProcessNode("check", NodeType.TASK, "Check \"order\""),
                        new ProcessNode("end", NodeType.END_EVENT, "Done")
                ),
                List.of(
                        new ProcessFlow("start", "check", null),
                        new ProcessFlow("check", "end", "Approved & complete")
                )
        );

        String xml = generator.generate(process);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );

        assertThat(document.getDocumentElement().getLocalName()).isEqualTo("definitions");
        assertThat(document.getElementsByTagNameNS("*", "task").getLength()).isEqualTo(1);
        assertThat(document.getElementsByTagNameNS("*", "sequenceFlow").getLength()).isEqualTo(2);
        assertThat(document.getElementsByTagNameNS("*", "incoming").getLength()).isEqualTo(2);
        assertThat(document.getElementsByTagNameNS("*", "outgoing").getLength()).isEqualTo(2);
        assertThat(xml).contains("Orders &amp; returns", "Order &lt;received&gt;");
    }
}
