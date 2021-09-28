/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.web.api;

import com.challenge.model.api.dto.CommitInfoPageDto;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.PATH;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;

@Path("/commit-viewer")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public interface CommitViewerResource {

    @GET
    @Path("/{user}/{repository}")
    @Operation(
        summary = "Get repo commit list",
        description = "Returns a list of commits for a repo."
    )
    @APIResponse(description = "List containing paginated commits",
                 responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = CommitInfoPageDto.class)))
    @APIResponse(description = "Bad Request",
                 responseCode = "400", content = @Content(schema = @Schema(implementation = String.class), example = "Request not valid"))
    @APIResponse(description = "Not Found",
                 responseCode = "403", content = @Content(schema = @Schema(implementation = String.class),
                                                          example = "Repository not found or you don't have the necessary permissions"))
    @APIResponse(description = "Request Timeout",
                 responseCode = "408", content = @Content(schema = @Schema(implementation = String.class), example = "Request Timeout"))
    @APIResponse(description = "Internal Server Error",
                 responseCode = "500", content = @Content(schema = @Schema(implementation = String.class), example = "An unknown error has occurred."))
    Response getCommitList(
        @PathParam("user")
        @Parameter(
        name = "user",
        description = "Name of the user that owns the repo",
        required = true,
        example = "pjcarrasqueira",
        in = PATH,
        schema = @Schema(type = SchemaType.STRING))
        String user,
        @PathParam("repository")
        @Parameter(
            name = "repository",
            description = "Name of the repo to get the commits",
            required = true,
            example = "commit-viewer",
            in = PATH,
            schema = @Schema(type = SchemaType.STRING))
            String repo,
        @QueryParam("page")
        @Parameter(
           name = "page",
           description = "Number of the requested page",
           example = "1",
           in = QUERY,
           schema = @Schema(type = SchemaType.INTEGER, minimum = "1", defaultValue = "1"))
        @DefaultValue("1")
        @Valid
        @Digits(integer = 10, fraction = 0)
        @Positive Integer page,
        @QueryParam("per_page")
        @Parameter(
           name = "per_page",
           description = "Max number of the items in the page",
           example = "50",
           in = QUERY,
           schema = @Schema(type = SchemaType.INTEGER, minimum = "1", maximum = "100", defaultValue = "5"))
        @DefaultValue("10")
        @Max(100)
        @Valid
        @Digits(integer = 3, fraction = 0)
        @Positive Integer perPage) throws IOException, InterruptedException;
}
