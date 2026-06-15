package com.autoresafety.api;

import com.autoresafety.api.dto.StepTwoModelCorrectionPayload;
import com.autoresafety.api.dto.StepTwoModelCorrectionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

@Path("/api/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public class AiResource {

    private static final Logger LOG = Logger.getLogger(AiResource.class);
    private static final String STEP_TWO_CORRECTION_CONTEXT = "Step 2 iStar/iStar4Safety model correction";

    @Inject
    @RestClient
    AiServiceClient aiServiceClient;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/ask")
    public Response ask(JsonNode payload) {
        try {
            return Response.ok(aiServiceClient.ask(payload)).build();
        } catch (ClientWebApplicationException exception) {
            return forwardUpstreamError(exception);
        } catch (ProcessingException exception) {
            LOG.error("AI service proxy failed", exception);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(unavailableError())
                    .build();
        }
    }

    @POST
    @Path("/correct-step-two-once")
    public Response correctStepTwoOnce(@Valid StepTwoModelCorrectionPayload payload) {
        ObjectNode request = JsonNodeFactory.instance.objectNode();
        request.put("question", buildStepTwoCorrectionPrompt(payload));
        request.put("context", firstNonBlank(payload.context(), STEP_TWO_CORRECTION_CONTEXT));

        try {
            JsonNode aiResponse = aiServiceClient.ask(request);
            JsonNode correctedModel = extractCorrectedModel(aiResponse);
            if (correctedModel == null) {
                return Response.status(422)
                        .entity(new StepTwoModelCorrectionResponse(
                                null,
                                "AI could not return a corrected model JSON in one pass. Alert the user and finalize manually."))
                        .build();
            }

            return Response.ok(new StepTwoModelCorrectionResponse(correctedModel, null)).build();
        } catch (ClientWebApplicationException exception) {
            return forwardUpstreamError(exception);
        } catch (ProcessingException exception) {
            LOG.error("AI service proxy failed", exception);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(unavailableError())
                    .build();
        }
    }

    private Response forwardUpstreamError(ClientWebApplicationException exception) {
        Response upstream = exception.getResponse();
        int status = upstream.getStatus();
        MediaType mediaType = upstream.getMediaType() != null ? upstream.getMediaType() : MediaType.APPLICATION_JSON_TYPE;
        String body = upstream.hasEntity() ? upstream.readEntity(String.class) : null;
        upstream.close();

        Response.ResponseBuilder builder = Response.status(status).type(mediaType);
        if (body != null && !body.isBlank()) {
            builder.entity(body);
        }
        return builder.build();
    }

    private ObjectNode unavailableError() {
        ObjectNode error = JsonNodeFactory.instance.objectNode();
        error.put("details", "AI service unavailable");
        return error;
    }

    private String buildStepTwoCorrectionPrompt(StepTwoModelCorrectionPayload payload) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are correcting a Step 2 iStar/iStar4Safety model.\n");
        prompt.append("Return valid JSON only: one complete corrected model object.\n");
        prompt.append("Do not return markdown, comments, explanations, prefixes, suffixes, or code fences.\n");
        prompt.append("Preserve existing ids when possible and prefer a valid full model over keeping invalid structures.\n");

        String context = trimToNull(payload.context());
        if (context != null) {
            prompt.append("Context: ").append(context).append("\n");
        }

        prompt.append("\npiStar validation errors:\n");
        for (int index = 0; index < payload.pistarErrors().size(); index++) {
            prompt.append(index + 1)
                    .append(". ")
                    .append(payload.pistarErrors().get(index).trim())
                    .append("\n");
        }

        prompt.append("\nCurrent full model JSON:\n");
        prompt.append(payload.model().toPrettyString());
        return prompt.toString();
    }

    private JsonNode extractCorrectedModel(JsonNode aiResponse) {
        if (aiResponse == null || aiResponse.isNull()) {
            return null;
        }

        JsonNode answer = aiResponse.get("answer");
        return extractModelNode(answer == null ? aiResponse : answer);
    }

    private JsonNode extractModelNode(JsonNode candidate) {
        if (candidate == null || candidate.isNull()) {
            return null;
        }

        if (candidate.isObject()) {
            if (looksLikeFullModel(candidate)) {
                return candidate;
            }

            JsonNode content = candidate.get("content");
            if (content != null) {
                JsonNode nested = extractModelNode(content);
                if (nested != null) {
                    return nested;
                }
            }

            JsonNode correctedModel = candidate.get("correctedModel");
            if (correctedModel != null) {
                JsonNode nested = extractModelNode(correctedModel);
                if (nested != null) {
                    return nested;
                }
            }

            return null;
        }

        if (candidate.isTextual()) {
            String rawText = trimToNull(candidate.asText());
            if (rawText == null) {
                return null;
            }

            JsonNode parsed = tryParseJson(rawText);
            if (parsed != null) {
                return extractModelNode(parsed);
            }

            int firstBrace = rawText.indexOf('{');
            int lastBrace = rawText.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                parsed = tryParseJson(rawText.substring(firstBrace, lastBrace + 1));
                if (parsed != null) {
                    return extractModelNode(parsed);
                }
            }
        }

        return null;
    }

    private JsonNode tryParseJson(String rawText) {
        try {
            return objectMapper.readTree(rawText);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private boolean looksLikeFullModel(JsonNode candidate) {
        return candidate.has("actors")
                && candidate.has("dependencies")
                && candidate.has("links")
                && candidate.has("display")
                && candidate.has("tool")
                && candidate.has("istar")
                && candidate.has("saveDate")
                && candidate.has("diagram");
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalizedPreferred = trimToNull(preferred);
        return normalizedPreferred != null ? normalizedPreferred : fallback;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
