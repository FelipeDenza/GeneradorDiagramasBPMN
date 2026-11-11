package com.desafiotecnicoimr.bpmn.domain;

import java.util.List;

public class ProcessValidationException extends RuntimeException {

    private final List<String> errors;

    public ProcessValidationException(List<String> errors) {
        super("The generated process is invalid");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
