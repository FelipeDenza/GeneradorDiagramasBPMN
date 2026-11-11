package com.desafiotecnicoimr.bpmn.application;

import org.springframework.stereotype.Component;

@Component
public class PromptPolicy {

    public void validate(String prompt) {
        for (int index = 0; index < prompt.length(); index++) {
            char character = prompt.charAt(index);
            if (Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t') {
                throw new InvalidPromptException("The prompt contains unsupported control characters");
            }
        }
    }
}
