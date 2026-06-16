package com.autoresafety.service;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Calls Google Gemini directly using the {@code com.google.genai} SDK.
 *
 * <p>This replaces the former standalone {@code autoresafety-ai-service} (port 8081):
 * the backend now talks to Gemini in-process instead of proxying over HTTP.</p>
 */
@ApplicationScoped
public class GeminiService {

    @ConfigProperty(name = "ai.gemini.model", defaultValue = "gemini-1.5-flash")
    String model;

    @ConfigProperty(name = "ai.gemini.api-key")
    Optional<String> apiKey;

    /**
     * Sends a single prompt to Gemini and returns the generated text.
     *
     * @param question the prompt to send; {@code null} is treated as empty
     * @return the model's text response, never {@code null}
     */
    public String ask(String question) {
        try (Client client = buildClient()) {
            GenerateContentResponse response = client.models.generateContent(
                model,
                question == null ? "" : question,
                null
            );

            return response == null ? "" : response.text();
        }
    }

    private Client buildClient() {
        String key = apiKey.orElse(null);
        if (key == null || key.isBlank()) {
            String envKey = System.getenv("GEMINI_API_KEY");
            if (envKey == null || envKey.isBlank()) {
                envKey = System.getenv("GOOGLE_API_KEY");
            }
            key = envKey;
        }

        if (key == null || key.isBlank()) {
            return new Client();
        }

        return Client.builder().apiKey(key).build();
    }
}
