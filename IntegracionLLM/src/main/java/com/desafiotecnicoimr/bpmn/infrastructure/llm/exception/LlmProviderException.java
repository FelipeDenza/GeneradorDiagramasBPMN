package com.desafiotecnicoimr.bpmn.infrastructure.llm.exception;

public class LlmProviderException extends RuntimeException {

    public enum Failure {
        AUTHENTICATION,
        RATE_LIMIT,
        TIMEOUT,
        REJECTED,
        INVALID_RESPONSE,
        UNAVAILABLE
    }

    private final String providerId;
    private final Failure failure;

    public LlmProviderException(String providerId, Failure failure) {
        this(providerId, failure, null);
    }

    public LlmProviderException(String providerId, Failure failure, Throwable cause) {
        super("LLM provider '" + providerId + "' failed: " + failure, cause);
        this.providerId = providerId;
        this.failure = failure;
    }

    public String getProviderId() {
        return providerId;
    }

    public Failure getFailure() {
        return failure;
    }
}
