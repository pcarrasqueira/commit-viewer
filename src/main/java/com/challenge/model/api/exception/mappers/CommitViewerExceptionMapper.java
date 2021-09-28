/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.exception.mappers;

import com.challenge.model.api.exception.CommitViewerException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Slf4j
public class CommitViewerExceptionMapper implements ExceptionMapper<CommitViewerException> {

    public CommitViewerExceptionMapper() {
    }

    public Response toResponse(final CommitViewerException exception) {
        log.error("{} exception caught: ", CommitViewerException.class.getName(), exception);
        return Response.status(exception.getErrorCode().getHttpStatusCode())
                       .entity(new JSONObject("{code: " + "\"" + exception.getErrorCode().getCode() + "\""
                                              + ", message: " + "\"" + exception.getMessage() + "\""
                                              + ", description: " + "\"" + exception.getDescription() + "\"" + "}").toString())
                       .build();
    }
}
