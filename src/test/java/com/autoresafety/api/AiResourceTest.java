package com.autoresafety.api;

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
}