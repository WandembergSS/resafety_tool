package com.autoresafety.api;

import com.autoresafety.service.GeminiService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(ByteBuddyExperimentalExtension.class)
@QuarkusTest
class AiResourceTest extends QuarkusTestBase {

    @InjectMock
    GeminiService geminiService;

    @Test
    void askWrapsGeminiAnswer() {
        when(geminiService.ask(anyString())).thenReturn("mocked");

        given()
                .contentType("application/json")
                .body("""
                        {"question":"ping"}
                        """)
                .when()
                .post("/api/ai/ask")
                .then()
                .statusCode(200)
                .body("answer", equalTo("mocked"));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).ask(promptCaptor.capture());
        org.hamcrest.MatcherAssert.assertThat(promptCaptor.getValue(), containsString("ping"));
    }

    @Test
    void askReturnsBadGatewayWhenGeminiFails() {
        when(geminiService.ask(anyString())).thenThrow(new RuntimeException("missing GOOGLE_API_KEY"));

        given()
                .contentType("application/json")
                .body("""
                        {"question":"ping"}
                        """)
                .when()
                .post("/api/ai/ask")
                .then()
                .statusCode(502)
                .body("details", equalTo("AI service unavailable"));
    }

    @Test
    void correctStepTwoOnceReturnsParsedCorrectedModel() {
        String correctedModel = """
                {
                    "actors": [],
                    "orphans": [],
                    "dependencies": [],
                    "links": [],
                    "display": {},
                    "tool": "pistar.2.0.0",
                    "istar": "2.0",
                    "saveDate": "Sun, 14 Jun 2026 22:29:30 GMT",
                    "diagram": {
                        "width": 2000,
                        "height": 1200,
                        "name": "Corrected",
                        "customProperties": {}
                    }
                }
                """;
        String geminiAnswer = JsonNodeFactory.instance.objectNode()
                .put("content", correctedModel)
                .put("summary", "corrected")
                .toString();
        when(geminiService.ask(anyString())).thenReturn(geminiAnswer);

        given()
                .contentType("application/json")
                .body("""
                        {
                            "model": {
                                "actors": [],
                                "orphans": [],
                                "dependencies": [],
                                "links": [],
                                "display": {},
                                "tool": "pistar.2.0.0",
                                "istar": "2.0",
                                "saveDate": "Sun, 14 Jun 2026 22:29:30 GMT",
                                "diagram": {
                                    "width": 2000,
                                    "height": 1200,
                                    "name": "Original",
                                    "customProperties": {}
                                }
                            },
                            "pistarErrors": [
                                "Safety element 'Hazard' must include a traceability ID.",
                                "Hazard 'Hazard' must obstruct at least one SafetyGoal."
                            ],
                            "context": "Step 2 iStar4Safety model correction"
                        }
                        """)
                .when()
                .post("/api/ai/correct-step-two-once")
                .then()
                .statusCode(200)
                .body("correctedModel.tool", equalTo("pistar.2.0.0"))
                .body("correctedModel.diagram.name", equalTo("Corrected"));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).ask(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        org.hamcrest.MatcherAssert.assertThat(prompt, containsString("piStar validation errors"));
        org.hamcrest.MatcherAssert.assertThat(prompt, containsString("Hazard 'Hazard' must obstruct at least one SafetyGoal."));
    }

    @Test
    void correctStepTwoOnceReturnsAlertWhenAiDoesNotReturnModelJson() {
        when(geminiService.ask(anyString())).thenReturn("still invalid");

        given()
                .contentType("application/json")
                .body("""
                        {
                            "model": {
                                "actors": [],
                                "orphans": [],
                                "dependencies": [],
                                "links": [],
                                "display": {},
                                "tool": "pistar.2.0.0",
                                "istar": "2.0",
                                "saveDate": "Sun, 14 Jun 2026 22:29:30 GMT",
                                "diagram": {
                                    "width": 2000,
                                    "height": 1200,
                                    "name": "Original",
                                    "customProperties": {}
                                }
                            },
                            "pistarErrors": [
                                "Safety element 'Hazard' must include a traceability ID."
                            ]
                        }
                        """)
                .when()
                .post("/api/ai/correct-step-two-once")
                .then()
                .statusCode(422)
                .body("alert", containsString("one pass"));
    }
}
