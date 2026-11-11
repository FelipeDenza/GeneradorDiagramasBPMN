package com.desafiotecnicoimr.bpmn.presentation;

import com.desafiotecnicoimr.bpmn.infrastructure.bpmn.BpmnGenerationException;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.UnknownProviderException;
import com.desafiotecnicoimr.bpmn.infrastructure.llm.exception.LlmProviderException;
import com.desafiotecnicoimr.bpmn.application.GeneratedProcessInvalidException;
import com.desafiotecnicoimr.bpmn.application.InvalidPromptException;
import com.desafiotecnicoimr.bpmn.application.NotProcessDescriptionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request is invalid", errors);
    }

    @ExceptionHandler(InvalidPromptException.class)
    ProblemDetail handleInvalidPrompt(InvalidPromptException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PROMPT", exception.getMessage(), List.of());
    }

    @ExceptionHandler(UnknownProviderException.class)
    ProblemDetail handleUnknownProvider(UnknownProviderException exception) {
        return problem(HttpStatus.BAD_REQUEST, "UNKNOWN_PROVIDER", exception.getMessage(), List.of());
    }

    @ExceptionHandler(GeneratedProcessInvalidException.class)
    ProblemDetail handleInvalidGeneratedProcess(GeneratedProcessInvalidException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "LLM_RESPONSE_INVALID",
                "The selected provider could not generate a valid BPMN process",
                List.of()
        );
    }

    @ExceptionHandler(NotProcessDescriptionException.class)
    ProblemDetail handleNotProcessDescription(NotProcessDescriptionException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "NOT_A_PROCESS_DESCRIPTION",
                "El texto no describe un proceso. Incluí actividades, participantes, decisiones y el resultado esperado.",
                List.of()
        );
    }

    @ExceptionHandler(LlmProviderException.class)
    ProblemDetail handleProviderFailure(LlmProviderException exception) {
        return switch (exception.getFailure()) {
            case AUTHENTICATION -> problem(
                    HttpStatus.BAD_GATEWAY,
                    "LLM_AUTHENTICATION_FAILED",
                    "The selected provider is not correctly configured",
                    List.of()
            );
            case RATE_LIMIT -> problem(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "LLM_RATE_LIMIT_EXCEEDED",
                    "The selected provider has reached its usage limit. Try again later or choose another provider",
                    List.of()
            );
            case TIMEOUT -> problem(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "LLM_PROVIDER_TIMEOUT",
                    "El proveedor no respondió dentro del tiempo de espera. Probá nuevamente o elegí otro proveedor.",
                    List.of()
            );
            case REJECTED -> problem(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "LLM_REQUEST_REJECTED",
                    "The selected provider rejected the process description",
                    List.of()
            );
            case INVALID_RESPONSE -> problem(
                    HttpStatus.BAD_GATEWAY,
                    "LLM_INVALID_RESPONSE",
                    "The selected provider returned an unreadable response",
                    List.of()
            );
            case UNAVAILABLE -> problem(
                    HttpStatus.BAD_GATEWAY,
                    "LLM_PROVIDER_UNAVAILABLE",
                    "The selected provider is temporarily unavailable",
                    List.of()
            );
        };
    }

    @ExceptionHandler(BpmnGenerationException.class)
    ProblemDetail handleBpmnGeneration(BpmnGenerationException exception) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "BPMN_GENERATION_FAILED",
                "The BPMN document could not be generated",
                List.of()
        );
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail, List<String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}
