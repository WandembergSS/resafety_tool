package com.autoresafety.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AiServiceStubTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String DEFAULT_BODY = "{\"answer\":\"mocked\"}";

    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();
    private static final AtomicInteger nextStatus = new AtomicInteger(200);
    private static final AtomicReference<String> nextBody = new AtomicReference<>(DEFAULT_BODY);

    private HttpServer server;

    @Override
    public Map<String, String> start() {
        reset();

        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        server.createContext("/api/ai/ask", this::handleAsk);
        server.start();
        return Map.of("quarkus.rest-client.ai-service.url", "http://localhost:" + server.getAddress().getPort() + "/api");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        reset();
    }

    static void setNextResponse(int status, String body) {
        nextStatus.set(status);
        nextBody.set(body);
    }

    static String lastMethod() {
        return lastMethod.get();
    }

    static String lastPath() {
        return lastPath.get();
    }

    static String lastBody() {
        return lastBody.get();
    }

    static void reset() {
        lastMethod.set(null);
        lastPath.set(null);
        lastBody.set(null);
        nextStatus.set(200);
        nextBody.set(DEFAULT_BODY);
    }

    private void handleAsk(HttpExchange exchange) throws IOException {
        lastMethod.set(exchange.getRequestMethod());
        lastPath.set(exchange.getRequestURI().getPath());
        lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        byte[] responseBytes = nextBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(nextStatus.get(), responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }
}