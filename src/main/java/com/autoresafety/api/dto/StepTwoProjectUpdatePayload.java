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
            Double x,
            Double y,
            List<String> isA,
            List<String> participatesIn,
            List<IntentionalElementPayload> intentionalElements,
            InternalLinksPayload internalLinks
    ) {
    }

    public record IntentionalElementPayload(
            String id,
            String name,
            String type,
            Double x,
            Double y,
            List<String> refinedBy,
            List<String> qualifies,
            String accidentLevel,
            String safetyGoalKind,
            List<String> obstructs
    ) {
    }

    public record InternalLinksPayload(
            List<Object> refinements,
            List<Object> contributions,
            List<Object> neededBy
    ) {
    }

    public record DependencyPayload(
            String id,
            String depender,
            String dependerElement,
            Double x,
            Double y,
            DependumPayload dependum,
            String dependee,
            String dependeeElement
    ) {
    }

    public record DependumPayload(
            String id,
            String name,
            String type
    ) {
    }
}