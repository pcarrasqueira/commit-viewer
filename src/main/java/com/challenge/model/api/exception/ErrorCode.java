/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.exception;

public interface ErrorCode {
    String getCode();
    String getMessage();
    int getHttpStatusCode();
}
