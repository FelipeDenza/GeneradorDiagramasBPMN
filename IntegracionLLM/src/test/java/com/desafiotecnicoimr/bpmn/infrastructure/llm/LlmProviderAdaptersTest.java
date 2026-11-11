package com.desafiotecnicoimr.bpmn.infrastructure.llm;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.prompt.ProcessSchema;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.gemini.GeminiLlmProvider;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.openai.groq.GroqLlmProvider;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.openai.openrouter.OpenRouterLlmProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LlmProviderAdaptersTest {

    private static final LlmRequest REQUEST = new LlmRequest(
            "System instruction",
            "Create the process",
            new ProcessSchema().value()
    );

    @Test
    void groqSendsStrictStructuredOutputAndExtractsContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GroqLlmProvider provider = new GroqLlmProvider(
                builder,
                new ObjectMapper(),
                "groq-key",
                "https://groq.test",
                "openai/gpt-oss-20b"
        );
        server.expect(requestTo("https://groq.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer groq-key"))
                .andExpect(content().string(containsString("\"strict\":true")))
                .andExpect(content().string(containsString("\"maxItems\":30")))
                .andExpect(content().string(containsString("\"maxItems\":60")))
                .andExpect(content().string(not(containsString("\"$schema\""))))
                .andRespond(withSuccess(openAiResponse(validProcess()), MediaType.APPLICATION_JSON));

        String result = provider.generate(REQUEST);

        assertThat(result).isEqualTo(validProcess());
        server.verify();
    }

    @Test
    void openRouterUsesFreeRouterAndBestEffortSchema() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterLlmProvider provider = new OpenRouterLlmProvider(
                builder,
                new ObjectMapper(),
                "openrouter-key",
                "https://openrouter.test",
                "openrouter/free"
        );
        server.expect(requestTo("https://openrouter.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer openrouter-key"))
                .andExpect(header("X-Title", "BPMN Prompt"))
                .andExpect(content().string(containsString("\"model\":\"openrouter/free\"")))
                .andExpect(content().string(containsString("\"strict\":false")))
                .andExpect(content().string(containsString("\"maxItems\":30")))
                .andExpect(content().string(containsString("\"maxItems\":60")))
                .andRespond(withSuccess(openAiResponse(validProcess()), MediaType.APPLICATION_JSON));

        assertThat(provider.generate(REQUEST)).isEqualTo(validProcess());
        server.verify();
    }

    @Test
    void geminiAdaptsSchemaAndExtractsCandidateText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiLlmProvider provider = new GeminiLlmProvider(
                builder,
                new ObjectMapper(),
                "gemini-key",
                "https://gemini.test",
                "gemini-3.8-flash"
        );
        server.expect(requestTo("https://gemini.test/models/gemini-3.8-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "gemini-key"))
                .andExpect(content().string(containsString("\"responseJsonSchema\"")))
                .andExpect(content().string(not(containsString("\"$schema\""))))
                .andExpect(content().string(not(containsString("\"pattern\""))))
                .andExpect(content().string(not(containsString("\"maxItems\""))))
                .andExpect(content().string(not(containsString("\"minItems\""))))
                .andExpect(content().string(containsString("\"nullable\":true")))
                .andRespond(withSuccess(geminiResponse(validProcess()), MediaType.APPLICATION_JSON));

        assertThat(provider.generate(REQUEST)).isEqualTo(validProcess());
        server.verify();
    }

    private static String openAiResponse(String content) {
        return """
                {"choices":[{"message":{"content":%s}}]}
                """.formatted(quote(content));
    }

    private static String geminiResponse(String content) {
        return """
                {"candidates":[{"content":{"parts":[{"text":%s}]}}]}
                """.formatted(quote(content));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String validProcess() {
        return """
                {"processName":"Simple","nodes":[{"key":"start","type":"START_EVENT","name":null},{"key":"end","type":"END_EVENT","name":null}],"flows":[{"source":"start","target":"end","name":null}]}
                """.strip();
    }
}
