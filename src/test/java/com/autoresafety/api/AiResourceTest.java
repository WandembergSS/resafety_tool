package com.autoresafety.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ByteBuddyExperimentalExtension.class)
@QuarkusTest
@QuarkusTestResource(AiServiceStubTestResource.class)
class AiResourceTest extends QuarkusTestBase {

    @AfterEach
    void resetStub() {
        AiServiceStubTestResource.reset();
    }

    @Test
    void askForwardsToAiService() {
        AiServiceStubTestResource.setNextResponse(200, "{\"answer\":\"mocked\"}");

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

        assertEquals("POST", AiServiceStubTestResource.lastMethod());
        assertEquals("/api/ai/ask", AiServiceStubTestResource.lastPath());
        org.hamcrest.MatcherAssert.assertThat(AiServiceStubTestResource.lastBody(), containsString("\"question\":\"ping\""));
    }

    @Test
    void askPropagatesAiServiceErrors() {
        AiServiceStubTestResource.setNextResponse(500, "{\"details\":\"missing GOOGLE_API_KEY\"}");

        given()
                .contentType("application/json")
                .body("""
                        {"question":"ping"}
                        """)
                .when()
                .post("/api/ai/ask")
                .then()
                .statusCode(500)
                .body("details", equalTo("missing GOOGLE_API_KEY"));
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
                ObjectNode answerWrapper = JsonNodeFactory.instance.objectNode();
                answerWrapper.put(
                                "answer",
                                JsonNodeFactory.instance.objectNode()
                                                .put("content", correctedModel)
                                                .put("summary", "corrected")
                                                .toString());
                AiServiceStubTestResource.setNextResponse(200, answerWrapper.toString());

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

                org.hamcrest.MatcherAssert.assertThat(AiServiceStubTestResource.lastBody(), containsString("piStar validation errors"));
                org.hamcrest.MatcherAssert.assertThat(AiServiceStubTestResource.lastBody(), containsString("Hazard 'Hazard' must obstruct at least one SafetyGoal."));
        }

        @Test
        void correctStepTwoOnceReturnsAlertWhenAiDoesNotReturnModelJson() {
                AiServiceStubTestResource.setNextResponse(200, "{\"answer\":\"still invalid\"}");

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