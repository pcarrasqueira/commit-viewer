/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.web.impl;

import com.challenge.model.api.exception.SkipFallbackException;
import com.challenge.service.api.GitApiService;
import com.challenge.service.cli.GitCliService;
import com.challenge.web.api.CommitViewerResource;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.inject.Inject;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class CommitViewerResourceImpl implements CommitViewerResource {

    @Inject
    GitCliService gitCliService;

    @Inject
    GitApiService gitApiService;

    @Override
    @Retry
    @Timeout()
    @Fallback(fallbackMethod = "getCommitListCli", skipOn = SkipFallbackException.class)
    public Response getCommitList(final String user,
                                  final String repo,
                                  final Integer page,
                                  final Integer perPage) throws InterruptedException {
        return Response.ok(gitApiService.getCommitList(user, repo, page, perPage)).build();
    }

    @Retry
    @Timeout
    public Response getCommitListCli(final String user,
                                     final String repo,
                                     final @Positive @Digits(integer = 10, fraction = 0) Integer page,
                                     final @Positive @Digits(integer = 3, fraction = 0) @Max(100) Integer perPage) throws IOException, InterruptedException {
        return Response.ok(gitCliService.getCommitList(user, repo, page, perPage)).build();
    }
}
