package com.desafiotecnicoimr.bpmn.infrastructure.llm.exception;

public class UnknownProviderException extends RuntimeException {

    public UnknownProviderException(String providerId) {
        super("Provider '" + providerId + "' is not available");
    }
}
