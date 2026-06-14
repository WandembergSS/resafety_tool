package com.autoresafety.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StepSevenUpdatedStepsUpdatePayload(
        @NotNull @Positive Long id,
        @Valid UpdatedStepsPayload updatedSteps
) {
    public record UpdatedStepsPayload(
            Boolean step1,
            Boolean step2,
            Boolean step3,
            Boolean step4,
            Boolean step5
    ) {
    }
}