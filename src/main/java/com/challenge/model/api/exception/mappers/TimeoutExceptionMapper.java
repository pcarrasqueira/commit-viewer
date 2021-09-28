/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.exception.mappers;

import com.challenge.model.api.exception.ErrorCodeImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.json.JSONObject;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Slf4j
public class TimeoutExceptionMapper implements ExceptionMapper<TimeoutException> {

    @Override
    public Response toResponse(final TimeoutException exception) {
        log.error("{} exception caught: ", TimeoutException.class.getName(), exception);
        return Response.status(ErrorCodeImpl.TIME_OUT.getHttpStatusCode())
                       .entity(new JSONObject("{code: " + "\"" + ErrorCodeImpl.TIME_OUT.getCode() + "\""
                                              + ", message: " + "\"" + ErrorCodeImpl.TIME_OUT.getMessage() + "\" }").toString())
                       .build();
    }
}
