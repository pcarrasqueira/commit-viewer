/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.exception.mappers;

import com.challenge.model.api.exception.ErrorCodeImpl;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


@Provider
@Slf4j
public class ConstraintExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(final ConstraintViolationException exception) {
        log.error("{} exception caught: {}", ConstraintViolationException.class.getName(), exception);
        return Response.status(ErrorCodeImpl.BAD_REQUEST.getHttpStatusCode())
                       .entity(new JSONObject("{code: " + "\"" + ErrorCodeImpl.BAD_REQUEST.getCode() + "\""
                                              + ", message: " + "\"" + ErrorCodeImpl.BAD_REQUEST.getMessage() + "\""
                                              + ", description: " + "\"Constraint violation(s) occurred during method validation\"" + "}").toString())
                       .build();

    }
}
