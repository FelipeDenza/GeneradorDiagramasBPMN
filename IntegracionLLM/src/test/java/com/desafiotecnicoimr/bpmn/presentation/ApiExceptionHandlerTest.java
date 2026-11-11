package com.desafiotecnicoimr.bpmn.presentation;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {
    @Test
    void returnsActionableGatewayTimeoutWithoutExposingInternalError() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new TimeoutController())
                .setControllerAdvice(new ApiExceptionHandler()).build();

        mvc.perform(get("/timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("LLM_PROVIDER_TIMEOUT"))
                .andExpect(jsonPath("$.detail").value(
                        "El proveedor no respondió dentro del tiempo de espera. Probá nuevamente o elegí otro proveedor."))
                .andExpect(jsonPath("$.cause").doesNotExist());
    }

    @RestController
    static class TimeoutController {
        @GetMapping("/timeout")
        public void fail() {
            throw new LlmProviderException("openrouter", LlmProviderException.Failure.TIMEOUT,
                    new RuntimeException("Internal transport details"));
        }
    }
}
