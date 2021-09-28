/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.service.api;

import com.challenge.model.api.dto.CommitInfoPageDto;
import com.challenge.model.api.dto.GitHubApiCommitDto;
import com.challenge.model.api.exception.CommitViewerException;
import com.challenge.model.api.exception.ErrorCodeImpl;
import com.challenge.model.api.exception.SkipFallbackException;
import com.challenge.model.api.mapper.CommitViewerMapper;
import com.challenge.service.common.CommitViewerHelper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.client.exception.ResteasyWebApplicationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
@Slf4j
public class GitApiService {

    @ConfigProperty(name = "commit-viewer.force-use-cli", defaultValue = "false")
    protected Boolean useCli;

    @Inject
    @RestClient
    GitHubApiClient gitHubApiClient;

    @Inject
    CommitViewerMapper commitViewerMapper;

    @Inject
    CommitViewerHelper commitViewerHelper;

    public CommitInfoPageDto getCommitList(final String user,
                                           final String repo,
                                           final Integer page,
                                           final Integer perPage) throws InterruptedException {
        log.info("CommitViewer : Getting commits using GitHub API");

        if (useCli) {
            log.debug("CommitViewer : Forcing use of CLI client");
            throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Forcing use of CLI client");
        }

        final List<GitHubApiCommitDto> gitHubApiCommitList;

        //Call GitHub API
        try {
            log.debug("CommitViewer : Calling GitHub API");
            gitHubApiCommitList = gitHubApiClient.getCommitsDto(user, repo, page, perPage);
        } catch (ResteasyWebApplicationException e) {
            log.error("CommitViewer : Error retrieving commits from GitHub API.");
            if (e.unwrap().getResponse().getStatus() == 404) {
                throw new SkipFallbackException(ErrorCodeImpl.NOT_FOUND, "Repository not found : " + commitViewerHelper.getRepoUrl(user, repo));
            } else {
                throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to contact GitHub API");
            }
        }

        log.debug("CommitViewer : Getting total number of commits");
        //TODO: I know, I know, 2 requests to get total commits. Didn't find other way to get it using GitHub API
        // It could be done in previous call, but than it will only work for calls inside the page range
        // and with an approximate value (get last page number from links * perPage)
        final Integer totalNumberOfCommits = commitViewerHelper.getTotalNumberOfCommitsFromLink(user, repo, perPage);

        return CommitInfoPageDto.builder()
                                .items(commitViewerMapper.gitHubApiCommitDtoListToCommitInfoDtoList(gitHubApiCommitList))
                                .page(page)
                                .perPage(perPage)
                                .count(gitHubApiCommitList.size())
                                .total(totalNumberOfCommits)
                                .totalPages((int) Math.ceil((double) totalNumberOfCommits / (double) perPage))
                                .build();
    }
}
