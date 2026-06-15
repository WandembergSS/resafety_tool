package com.autoresafety.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
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

    @Inject
    @RestClient
    AiServiceClient aiServiceClient;

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
}
