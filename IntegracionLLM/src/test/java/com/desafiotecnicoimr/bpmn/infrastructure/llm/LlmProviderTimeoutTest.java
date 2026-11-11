package com.desafiotecnicoimr.bpmn.infrastructure.llm;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmProviderException;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.prompt.ProcessSchema;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.gemini.GeminiLlmProvider;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.openai.groq.GroqLlmProvider;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.openai.openrouter.OpenRouterLlmProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;

class LlmProviderTimeoutTest {
    private static final LlmRequest REQUEST = new LlmRequest("System", "Process", new ProcessSchema().value());

    private static final List<Function<RestClient.Builder, LlmProvider>> PROVIDERS = List.of(
            builder -> new OpenRouterLlmProvider(builder, new ObjectMapper(), "test-key", "https://provider.test", "free"),
            builder -> new GroqLlmProvider(builder, new ObjectMapper(), "test-key", "https://provider.test", "model"),
            builder -> new GeminiLlmProvider(builder, new ObjectMapper(), "test-key", "https://provider.test", "model")
    );

    @Test
    void classifiesTimeoutBeforeResponseHeaders() {
        for (var factory : PROVIDERS) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            var provider = factory.apply(builder);
            server.expect(anything()).andRespond(withException(new SocketTimeoutException("Read timed out")));

            assertTimeout(provider);
            server.verify();
        }
    }

    @Test
    void classifiesTimeoutWhileReadingResponseBody() {
        for (var factory : PROVIDERS) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            var provider = factory.apply(builder);
            server.expect(anything()).andRespond(request -> {
                var response = new MockClientHttpResponse(new InputStream() {
                    @Override
                    public int read() throws SocketTimeoutException {
                        throw new SocketTimeoutException("Read timed out");
                    }
                }, HttpStatus.OK);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return response;
            });

            assertTimeout(provider);
            server.verify();
        }
    }

    private void assertTimeout(LlmProvider provider) {
        Throwable failure = catchThrowable(() -> provider.generate(REQUEST));
        assertThat(failure).as(provider.descriptor().id()).isInstanceOf(LlmProviderException.class);
        assertThat(((LlmProviderException) failure).getFailure().name()).isEqualTo("TIMEOUT");
    }
}
