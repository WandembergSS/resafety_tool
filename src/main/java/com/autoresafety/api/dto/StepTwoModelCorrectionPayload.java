package com.autoresafety.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StepTwoModelCorrectionPayload(
        @NotNull JsonNode model,
        @NotEmpty List<@NotBlank String> pistarErrors,
        String context
) {
}