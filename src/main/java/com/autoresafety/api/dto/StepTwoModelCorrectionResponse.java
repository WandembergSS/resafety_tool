package com.autoresafety.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StepTwoModelCorrectionResponse(
        JsonNode correctedModel,
        String alert
) {
}