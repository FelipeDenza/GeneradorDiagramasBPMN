package com.desafiotecnicoimr.bpmn.infrastructure.llm.exception;

import java.util.List;

public class LlmResponseInvalidException extends RuntimeException {

    private final List<String> errors;

    public LlmResponseInvalidException(String error, Throwable cause) {
        super("The LLM response is not valid structured output", cause);
        this.errors = List.of(error);
    }

    public List<String> getErrors() {
        return errors;
    }
}
