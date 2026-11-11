package com.desafiotecnicoimr.bpmn.infrastructure.llm.response;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmResponseInvalidException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser(new ObjectMapper());

    @Test
    void parsesCanonicalProcessJson() {
        String response = """
                {
                  "isProcessDescription": true,
                  "reason": null,
                  "process": {
                    "processName": "Simple process",
                    "nodes": [
                      {"key": "start", "type": "START_EVENT", "name": "Start"},
                      {"key": "end", "type": "END_EVENT", "name": "End"}
                    ],
                    "flows": [{"source": "start", "target": "end"}]
                  }
                }
                """;

        assertThat(parser.parse(response).process().nodes()).hasSize(2);
    }

    @Test
    void rejectsMarkdownAndUnknownProperties() {
        String response = """
                {
                  "isProcessDescription": false,
                  "reason": "Not a process",
                  "process": null,
                  "explanation": "extra output"
                }
                """;

        assertThatThrownBy(() -> parser.parse(response))
                .isInstanceOf(LlmResponseInvalidException.class);
    }
}
