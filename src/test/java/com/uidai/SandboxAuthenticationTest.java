package com.uidai;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxAuthenticationTest {

    private static final String AUTH_PATH =
            "/api/v1/sandbox/auth";

    private static String apiKey;

    @BeforeAll
    static void configure() {

        RestAssured.baseURI = System.getenv().getOrDefault(
                "SANDBOX_BASE_URL",
                "http://localhost:8080"
        );

        apiKey = System.getenv("SANDBOX_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "SANDBOX_API_KEY must be configured"
            );
        }
    }

    // Generate unique test data for every execution.
    private static Map<String, Object> validPayload() {

        String runId = UUID.randomUUID().toString();

        return Map.of(
                "api_key", apiKey,
                "mock_id_number", "MOCK-" + runId,
                "device_data", Map.of(
                        "device_id", "DEVICE-" + runId,
                        "biometric_hash", "test-biometric-hash"
                )
        );
    }

    // Test 1: Valid authentication request.
    @Test
    void shouldAuthenticateValidSandboxRequest() {

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(validPayload())

        .when()
                .post(AUTH_PATH)

        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("auth_token", not(isEmptyOrNullString()))
                .body("auth_token", instanceOf(String.class));
    }

    // Test 2: Replay attack protection.
    @Test
    void shouldRejectReplayWithinOneHundredMilliseconds() {

        Map<String, Object> payload = validPayload();

        long firstRequestStart = System.nanoTime();

        Response firstResponse = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(payload)

        .when()
                .post(AUTH_PATH);

        long firstRequestEnd = System.nanoTime();

        assertTrue(
                firstResponse.statusCode() == 200,
                "First request must succeed"
        );

        Response replayResponse = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(payload)

        .when()
                .post(AUTH_PATH);

        long replayEnd = System.nanoTime();

        long elapsedMillis =
                (replayEnd - firstRequestStart) / 1_000_000;

        assertTrue(
                elapsedMillis < 100,
                "Both requests were not completed within 100 ms. "
                        + "Elapsed: " + elapsedMillis + " ms"
        );

        assertTrue(
                replayResponse.statusCode() == 401
                        || replayResponse.statusCode() == 409,
                "Expected 401 or 409 for replay attack, but received "
                        + replayResponse.statusCode()
        );
}
}
