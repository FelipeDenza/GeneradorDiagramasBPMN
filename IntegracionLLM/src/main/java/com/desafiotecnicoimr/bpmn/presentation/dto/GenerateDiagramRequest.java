package com.desafiotecnicoimr.bpmn.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateDiagramRequest(
        @NotBlank(message = "prompt is required")
        @Size(max = 4000, message = "prompt must not exceed 4000 characters")
        String prompt,

        @NotBlank(message = "provider is required")
        @Size(max = 50, message = "provider must not exceed 50 characters")
        String provider
) {
}
