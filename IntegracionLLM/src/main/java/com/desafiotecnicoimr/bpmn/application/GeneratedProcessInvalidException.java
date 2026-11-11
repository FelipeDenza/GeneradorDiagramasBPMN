package com.desafiotecnicoimr.bpmn.application;

public class GeneratedProcessInvalidException extends RuntimeException {

    public GeneratedProcessInvalidException(Throwable cause) {
        super("The provider could not generate a valid process", cause);
    }
}
