/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class HealthCheckIT {
    @Test
    void testHealthCheck() {
        given()
            .get("/q/health")
            .then()
            .statusCode(OK.getStatusCode())
            .body("checks.status", everyItem(is("UP")));
    }
}
