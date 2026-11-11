package com.desafiotecnicoimr.bpmn.infrastructure.llm.support;

import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmProviderException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

public final class ProviderHttpErrors {

    private ProviderHttpErrors() {
    }

    public static LlmProviderException translate(String providerId, RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401 || status == 403) {
            return new LlmProviderException(
                    providerId,
                    LlmProviderException.Failure.AUTHENTICATION,
                    exception
            );
        }
        if (status == 429) {
            return new LlmProviderException(providerId, LlmProviderException.Failure.RATE_LIMIT, exception);
        }
        if (status == 408 || status == 504) {
            return new LlmProviderException(providerId, LlmProviderException.Failure.TIMEOUT, exception);
        }
        return new LlmProviderException(providerId, LlmProviderException.Failure.UNAVAILABLE, exception);
    }

    public static LlmProviderException translate(String providerId, RestClientException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return new LlmProviderException(providerId, LlmProviderException.Failure.TIMEOUT, exception);
            }
        }
        var failure = exception instanceof ResourceAccessException
                ? LlmProviderException.Failure.UNAVAILABLE
                : LlmProviderException.Failure.INVALID_RESPONSE;
        return new LlmProviderException(providerId, failure, exception);
    }
}
