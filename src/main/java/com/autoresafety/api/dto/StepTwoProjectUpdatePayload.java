package com.autoresafety.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record StepTwoProjectUpdatePayload(
        @NotNull @Positive Long id,
        @Valid Step2Information step2Information
) {
    public record Step2Information(
            String modelName,
            List<ActorPayload> actors,
            List<DependencyPayload> dependencies
    ) {
    }

    public record ActorPayload(
            String id,
            String name,
            String type,
            List<IntentionalElementPayload> intentionalElements
    ) {
    }

    public record IntentionalElementPayload(
            String id,
            String name,
            String type
    ) {
    }

    public record DependencyPayload(
            String id,
            String depender,
            DependumPayload dependum
    ) {
    }

    public record DependumPayload(
            String id,
            String name,
            String type
    ) {
    }
}