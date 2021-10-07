/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.service.api;

import com.challenge.model.api.dto.CommitInfoDto;
import com.challenge.model.api.dto.CommitInfoPageDto;
import com.challenge.model.api.exception.CommitViewerException;
import com.challenge.model.api.exception.ErrorCodeImpl;
import com.challenge.model.mapper.CommitViewerMapper;
import com.challenge.service.common.CommitViewerHelper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
@Slf4j
public class GitApiService {

    @ConfigProperty(name = "commit-viewer.force-use-cli", defaultValue = "false")
    protected Boolean useCli;

    @Inject
    CommitViewerMapper commitViewerMapper;

    @Inject
    CommitViewerHelper commitViewerHelper;


    public CommitInfoPageDto getCommitList(final String user,
                                           final String repo,
                                           final Integer page,
                                           final Integer perPage) throws InterruptedException {
        log.info("CommitViewer : Getting commits using GitHub API");

        long totalNumberOfCommits = commitViewerHelper.getTotalNumberOfCommitsFromDb(user, repo);
        final List<CommitInfoDto> commitInfoDtoList;
        Integer totalPages = 0;

        if (useCli) {
            log.debug("CommitViewer : Forcing use of CLI client");
            throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Forcing use of CLI client");
        }

        //Fetch data from db
        if (totalNumberOfCommits > 0) {
            log.info("CommitViewer : Fetching data from database");
            totalPages = (int) Math.ceil((double) totalNumberOfCommits / (double) perPage);
            commitInfoDtoList = commitViewerHelper.getCommitsFromDb(user, repo, page, perPage);

        } else { //Call API and save data
            log.info("CommitViewer : No data on database for the repo " + commitViewerHelper.getRepoUrl(user, repo) + " will call GitHub API"
                     + "and persist data on DB.");
            log.debug("CommitViewer : Getting total number of commits");
            //TODO: I know, I know, 2 requests to get total commits. Didn't find other way to get it using GitHub API
            // It could be done in previous call, but than it will only work for calls inside the page range
            // and with an approximate value (get last page number from links * perPage)
            totalNumberOfCommits = commitViewerHelper.getTotalNumberOfCommitsFromLink(user, repo, perPage);

            totalPages = (int) Math.ceil((double) totalNumberOfCommits / (double) perPage);

            //TODO: Deal with API with rate limit (check X-RateLimit-Limit and X-RateLimit-Remaining headers or use
            // GET https://api.github.com/rate_limit to get limit remaining) do not make the calls if remaining limit is not enough
            // return 403 "message": "API rate limit exceeded for xxx.xxx.xxx.xxx..." on limit exceeded
            // "For unauthenticated requests, the rate limit allows for up to 60 requests per hour"
            if (totalPages > 60) { // || remainingRequests < totalPages )
                log.debug("This request exceeds the unauthorized requests rate limit for GitHub API (60)");
                throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR,
                                                "Total remaining requests is not enough to make this request");
            }

            commitInfoDtoList = commitViewerHelper.getAllCommitsFromApiAndPersistOnDb(totalPages, user, repo, page, perPage);
        }

        return CommitInfoPageDto.builder()
                                .items(commitInfoDtoList)
                                .page(page)
                                .perPage(perPage)
                                .count(commitInfoDtoList.size())
                                .total(totalNumberOfCommits)
                                .totalPages(totalPages)
                                .build();
    }
}
