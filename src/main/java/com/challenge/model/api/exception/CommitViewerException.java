/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.exception;

import lombok.Data;

@Data
public class CommitViewerException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String description;

    public CommitViewerException(final ErrorCode error) {
        this(error, null);
    }

    public CommitViewerException(final ErrorCode error, final String desc) {
        this(error, desc, null);
    }

    public CommitViewerException(final ErrorCode error, final String desc, final Throwable cause) {
        super(error.getMessage(), cause);
        this.errorCode = error;
        this.description = desc;
    }
}
