/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.HttpURLConnection;

import static lombok.AccessLevel.PRIVATE;

@Getter
@AllArgsConstructor(access = PRIVATE)
public enum ErrorCodeImpl implements ErrorCode {

    UNKNOWN_ERROR("1", "An unknown error has occurred.", HttpURLConnection.HTTP_INTERNAL_ERROR),
    NOT_FOUND("2", "Repository not found or you don't have the necessary permissions", HttpURLConnection.HTTP_NOT_FOUND),
    BAD_REQUEST("3", "Request not valid", HttpURLConnection.HTTP_BAD_REQUEST),
    TIME_OUT("4", "Request timeout", HttpURLConnection.HTTP_CLIENT_TIMEOUT);

    private final String code;
    private final String message;
    private final int httpStatusCode;
}
