/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge;

import com.challenge.model.api.dto.CommitInfoDto;
import com.challenge.model.api.dto.CommitInfoPageDto;
import com.challenge.model.api.exception.CommitViewerException;
import com.challenge.model.api.exception.ErrorCodeImpl;
import com.challenge.model.api.exception.SkipFallbackException;
import com.challenge.service.api.GitApiService;
import com.challenge.service.cli.GitCliService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.config.RestAssuredConfig;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CommitViewerResourceMockitoIT {

    private static final String RESOURCE_URL = "/commit-viewer/{user}/{repo}";

    @Inject
    RestAssuredConfig config;

    @InjectMock
    GitApiService gitApiService;

    @InjectMock
    GitCliService gitCliService;

    @Test
    public void getCommitsApiTimeout() throws IOException, InterruptedException {

        final List<CommitInfoDto> list = new ArrayList<>();
        list.add(CommitInfoDto.of("ce206ff99e275fd99d57d1e024cc22e3db9ba282",
                                  "Add new line\n\nAdded new line on readme",
                                  Instant.parse("2021-09-26T21:55:26Z"),
                                  "Paulo Carrasqueira <pjcarrasqueira@gmail.com>"));


        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(TimeoutException.class);
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(
            CommitInfoPageDto.builder()
                             .page(1)
                             .perPage(10)
                             .count(4)
                             .total(4)
                             .totalPages(1)
                             .items(list)
                             .build());

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(OK.getStatusCode())
            .body("items[0].sha", is(list.get(0).getSha()),
                  "items[0].message", is(list.get(0).getMessage()),
                  "items[0].date", is(list.get(0).getDate().toString()),
                  "items[0].author", is(list.get(0).getAuthor()),
                  "page", is(1),
                  "count", is(4),
                  "per_page", is(10),
                  "total", is(4),
                  "total_pages", is(1));
    }

    @Test
    public void getCommitsApiCommitViewerException() throws IOException, InterruptedException {

        final List<CommitInfoDto> list = new ArrayList<>();
        list.add(CommitInfoDto.of("ce206ff99e275fd99d57d1e024cc22e3db9ba282",
                                  "Add new line\n\nAdded new line on readme",
                                  Instant.parse("2021-09-26T21:55:26Z"),
                                  "Paulo Carrasqueira <pjcarrasqueira@gmail.com>"));


        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(CommitViewerException.class);
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenReturn(
            CommitInfoPageDto.builder()
                             .page(1)
                             .perPage(10)
                             .count(4)
                             .total(4)
                             .totalPages(1)
                             .items(list)
                             .build());

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(OK.getStatusCode())
            .body("items[0].sha", is(list.get(0).getSha()),
                  "items[0].message", is(list.get(0).getMessage()),
                  "items[0].date", is(list.get(0).getDate().toString()),
                  "items[0].author", is(list.get(0).getAuthor()),
                  "page", is(1),
                  "count", is(4),
                  "per_page", is(10),
                  "total", is(4),
                  "total_pages", is(1));
    }

    @Test
    public void getCommitsTimeOutException() throws IOException, InterruptedException {

        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(TimeoutException.class);
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(TimeoutException.class);

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(ErrorCodeImpl.TIME_OUT.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.TIME_OUT.getCode()),
                  "message", is(ErrorCodeImpl.TIME_OUT.getMessage()));
    }

    @Test
    public void getCommitsConstraintViolationException() throws IOException, InterruptedException {

        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(ConstraintViolationException.class);
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(ConstraintViolationException.class);

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(ErrorCodeImpl.BAD_REQUEST.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.BAD_REQUEST.getCode()),
                  "message", is(ErrorCodeImpl.BAD_REQUEST.getMessage()),
                  "description", is("Constraint violation(s) occurred during method validation"));
    }

    @Test
    public void getCommitsSkipFallbackException() throws IOException, InterruptedException {

        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new SkipFallbackException(ErrorCodeImpl.NOT_FOUND, "API"));
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new SkipFallbackException(ErrorCodeImpl.NOT_FOUND, "CLI"));

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(ErrorCodeImpl.NOT_FOUND.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.NOT_FOUND.getCode()),
                  "message", is(ErrorCodeImpl.NOT_FOUND.getMessage()),
                  "description", is("API"));
    }

    @Test
    public void getCommitsCommitViewerExceptionUnknown() throws IOException, InterruptedException {

        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "API"));
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "CLI"));

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(ErrorCodeImpl.UNKNOWN_ERROR.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.UNKNOWN_ERROR.getCode()),
                  "message", is(ErrorCodeImpl.UNKNOWN_ERROR.getMessage()),
                  "description", is("CLI"));
    }

    @Test
    public void getCommitsCommitViewerExceptionBadRequest() throws IOException, InterruptedException {

        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new CommitViewerException(ErrorCodeImpl.BAD_REQUEST, "API"));
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new CommitViewerException(ErrorCodeImpl.BAD_REQUEST, "CLI"));

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(ErrorCodeImpl.BAD_REQUEST.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.BAD_REQUEST.getCode()),
                  "message", is(ErrorCodeImpl.BAD_REQUEST.getMessage()),
                  "description", is("CLI"));
    }

    @Test
    public void getCommitsCommitViewerExceptionNotFound() throws IOException, InterruptedException {

        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new CommitViewerException(ErrorCodeImpl.NOT_FOUND, "API"));
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new CommitViewerException(ErrorCodeImpl.NOT_FOUND, "CLI"));

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(ErrorCodeImpl.NOT_FOUND.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.NOT_FOUND.getCode()),
                  "message", is(ErrorCodeImpl.NOT_FOUND.getMessage()),
                  "description", is("CLI"));
    }

    @Test
    public void getCommitsCommitViewerExceptionTimeout() throws IOException, InterruptedException {

        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new CommitViewerException(ErrorCodeImpl.TIME_OUT, "API"));
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                                                 ArgumentMatchers.any())).thenThrow(new CommitViewerException(ErrorCodeImpl.TIME_OUT, "CLI"));

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(ErrorCodeImpl.TIME_OUT.getHttpStatusCode())
            .body("code", is(ErrorCodeImpl.TIME_OUT.getCode()),
                  "message", is(ErrorCodeImpl.TIME_OUT.getMessage()),
                  "description", is("CLI"));
    }

    @Test
    public void getCommitsApiResponse() throws IOException, InterruptedException {

        final List<CommitInfoDto> apiList = new ArrayList<>();
        apiList.add(CommitInfoDto.of("ce206ff99e275fd99d57d1e024cc22e3db9ba282",
                                  "api",
                                  Instant.parse("2021-09-26T21:55:26Z"),
                                  "Paulo Carrasqueira <pjcarrasqueira@gmail.com>"));

        final List<CommitInfoDto> cliList = new ArrayList<>();
        cliList.add(CommitInfoDto.of("ce206ff99e275fd99d57d1e024cc22e3db9ba283",
                                  "cli",
                                  Instant.parse("2021-09-26T21:55:26Z"),
                                  "Paulo Carrasqueira <pjcarrasqueira@gmail.com>"));

        Mockito.when(gitApiService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(
            CommitInfoPageDto.builder()
                             .page(1)
                             .perPage(10)
                             .count(4)
                             .total(4)
                             .totalPages(1)
                             .items(apiList)
                             .build());
        Mockito.when(gitCliService.getCommitList(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(
            CommitInfoPageDto.builder()
                             .page(1)
                             .perPage(10)
                             .count(4)
                             .total(4)
                             .totalPages(1)
                             .items(cliList)
                             .build());

        given()
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .get(RESOURCE_URL, "pcarrasqueira", "commit-viewer-test")
            .then()
            .statusCode(OK.getStatusCode())
            .body("items[0].sha", is(apiList.get(0).getSha()),
                  "items[0].message", is(apiList.get(0).getMessage()),
                  "items[0].date", is(apiList.get(0).getDate().toString()),
                  "items[0].author", is(apiList.get(0).getAuthor()),
                  "page", is(1),
                  "count", is(4),
                  "per_page", is(10),
                  "total", is(4),
                  "total_pages", is(1));
    }
}
