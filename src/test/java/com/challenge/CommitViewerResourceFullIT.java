/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge;

import com.challenge.model.api.exception.ErrorCodeImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CommitViewerResourceFullIT {

    private static final String RESOURCE_URL = "/commit-viewer/{user}/{repo}";
    private static final String RESOURCE_URL_PAGE_PER_PAGE = "/commit-viewer/{user}/{repo}?page={page}&per_page={per_page}";

    @Inject
    RestAssuredConfig config;

    @Test
    public void getCommitsOkDefaultValues() {
        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(OK.getStatusCode())
            .body("items[0].sha", is("ce206ff99e275fd99d57d1e024cc22e3db9ba282"),
                  "items[0].message", is("Add new line\n\nAdded new line on readme"),
                  "items[0].date", is("2021-09-26T21:55:26Z"),
                  "items[0].author", is("Paulo Carrasqueira <pjcarrasqueira@gmail.com>"),
                  "items[1].sha", is("b56845e4fc80a8eec0c873708f72e9a55b6d2668"),
                  "items[1].message", is("Secondo commit"),
                  "items[1].date", is("2021-09-26T21:54:43Z"),
                  "items[1].author", is("Paulo Carrasqueira <pjcarrasqueira@gmail.com>"),
                  "items[2].sha", is("f11df53eb3682d2536db8b717cc4a820090a5876"),
                  "items[2].message", is("Update readme\n\nFirst commit"),
                  "items[2].date", is("2021-09-26T21:54:19Z"),
                  "items[2].author", is("Paulo Carrasqueira <pjcarrasqueira@gmail.com>"),
                  "items[3].sha", is("7d54a55f60082c42dbdb8e586cbcb15023971922"),
                  "items[3].message", is("Initial commit"),
                  "items[3].date", is("2021-09-26T21:53:49Z"),
                  "items[3].author", is("Paulo Carrasqueira <pjcarrasqueira@gmail.com>"),
                  "page", is(1),
                  "count", is(4),
                  "per_page", is(10),
                  "total", is(4),
                  "total_pages", is(1));
    }

    @Test
    public void getCommitsOkPage1PerPage1() {
        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL_PAGE_PER_PAGE, "pcarrasqueira", "commit-viewer-test", 1, 1)
            .then()
            .statusCode(OK.getStatusCode())
            .body("items[0].sha", is("ce206ff99e275fd99d57d1e024cc22e3db9ba282"),
                  "items[0].message", is("Add new line\n\nAdded new line on readme"),
                  "items[0].date", is("2021-09-26T21:55:26Z"),
                  "items[0].author", is("Paulo Carrasqueira <pjcarrasqueira@gmail.com>"),
                  "page", is(1),
                  "count", is(1),
                  "per_page", is(1),
                  "total", is(4),
                  "total_pages", is(4));
    }

    @Test
    public void getCommitsOkPage2PerPage3() {
        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL_PAGE_PER_PAGE, "pcarrasqueira", "commit-viewer-test", 2, 3)
            .then()
            .statusCode(OK.getStatusCode())
            .body("items[0].sha", is("7d54a55f60082c42dbdb8e586cbcb15023971922"),
                  "items[0].message", is("Initial commit"),
                  "items[0].date", is("2021-09-26T21:53:49Z"),
                  "items[0].author", is("Paulo Carrasqueira <pjcarrasqueira@gmail.com>"),
                  "page", is(2),
                  "count", is(1),
                  "per_page", is(3),
                  "total", is(4),
                  "total_pages", is(2));
    }

    @Test
    public void getCommitsInvalidPage() {
        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL_PAGE_PER_PAGE, "pcarrasqueira", "commit-viewer-test", -1, 1)
            .then()
            .statusCode(ErrorCodeImpl.BAD_REQUEST.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.BAD_REQUEST.getCode()),
                  "message", is(ErrorCodeImpl.BAD_REQUEST.getMessage()));
    }

    @Test
    public void getCommitsInvalidPerPage() {
        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL_PAGE_PER_PAGE, "pcarrasqueira", "commit-viewer-test", 1, -1)
            .then()
            .statusCode(ErrorCodeImpl.BAD_REQUEST.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.BAD_REQUEST.getCode()),
                  "message", is(ErrorCodeImpl.BAD_REQUEST.getMessage()));
    }

    @Test
    public void getCommitsPerPage0() {
        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL_PAGE_PER_PAGE, "pcarrasqueira", "commit-viewer-test", 1, 0)
            .then()
            .statusCode(ErrorCodeImpl.BAD_REQUEST.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.BAD_REQUEST.getCode()),
                  "message", is(ErrorCodeImpl.BAD_REQUEST.getMessage()));
    }

    @Test
    public void getCommitsPage0() {
        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL_PAGE_PER_PAGE, "pcarrasqueira", "commit-viewer-test", 1, 0)
            .then()
            .statusCode(ErrorCodeImpl.BAD_REQUEST.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.BAD_REQUEST.getCode()),
                  "message", is(ErrorCodeImpl.BAD_REQUEST.getMessage()));
    }

    //TODO: add more tests for database scenarios
}
