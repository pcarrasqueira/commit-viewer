/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.exception;

import lombok.Data;

@Data
public class SkipFallbackException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String description;

    public SkipFallbackException(final ErrorCode error) {
        this(error, null);
    }

    public SkipFallbackException(final ErrorCode error, final String desc) {
        this(error, desc, null);
    }

    public SkipFallbackException(final ErrorCode error, final String desc, final Throwable cause) {
        super(error.getMessage(), cause);
        this.errorCode = error;
        this.description = desc;
    }
}
