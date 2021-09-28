/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge;

import com.challenge.model.api.exception.ErrorCodeImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ErrorCodeIT {
    @Test
    public void testUniqueErrorCodes() {
        final List<String> codes = new ArrayList<>(ErrorCodeImpl.values().length);
        for (final ErrorCodeImpl error : ErrorCodeImpl.values()) {
            final String code = error.getCode().trim().toLowerCase(Locale.US);
            Assertions.assertFalse(codes.contains(code),
                                   "Repeated error code: " + error.getCode());
            codes.add(code);
        }
    }
}
