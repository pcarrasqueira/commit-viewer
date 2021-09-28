/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.service.api;

import com.challenge.model.api.dto.GitHubApiCommitDto;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
@RegisterRestClient(configKey = "github-api")
public interface GitHubApiClient {

    @GET
    @Path("/repos/{owner}/{repo}/commits")
    @Produces(APPLICATION_JSON)
    List<GitHubApiCommitDto> getCommitsDto(
        @PathParam("owner") String owner,
        @PathParam("repo") String repo,
        @QueryParam("page") Integer page,
        @QueryParam("per_page") Integer perPage);

    @GET
    @Path("/repos/{owner}/{repo}/commits")
    @Produces(APPLICATION_JSON)
    Response getCommitsResponse(
        @PathParam("owner") String owner,
        @PathParam("repo") String repo,
        @QueryParam("page") Integer page,
        @QueryParam("per_page") Integer perPage);
}
