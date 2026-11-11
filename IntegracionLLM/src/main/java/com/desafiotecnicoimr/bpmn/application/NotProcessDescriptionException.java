package com.desafiotecnicoimr.bpmn.application;

public class NotProcessDescriptionException extends RuntimeException {

    public NotProcessDescriptionException() {
        super("The prompt does not describe a business process");
    }
}
