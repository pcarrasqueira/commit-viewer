/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.service.cli;

import com.challenge.model.api.dto.CommitInfoDto;
import com.challenge.model.api.dto.CommitInfoPageDto;
import com.challenge.model.api.exception.CommitViewerException;
import com.challenge.model.api.exception.ErrorCodeImpl;
import com.challenge.service.common.CommitViewerHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.context.ManagedExecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Slf4j
public class GitCliService {

    private static final long futureTimeout = 60000;

    @Inject
    CommitViewerHelper commitViewerHelper;

    @Inject
    ManagedExecutor managedExecutor;

    public CommitInfoPageDto getCommitList(final String user,
                                           final String repo,
                                           final Integer page,
                                           final Integer perPage) throws IOException {
        log.info("CommitViewer : Request using GitHub API failed, will use the git CLI as fallback");
        log.debug("CommitViewer : Starting git clone");

        long totalNumberOfCommits = commitViewerHelper.getTotalNumberOfCommitsFromDb(user, repo);

        final List<CommitInfoDto> commitInfoDtoList;

        //Fetch data from database
        if (totalNumberOfCommits > 0) {
            log.info("CommitViewer : Fetching data from database");
            commitInfoDtoList = commitViewerHelper.getCommitsFromDb(user, repo, page, perPage);

        } else { //clone repo and save data
            log.info("CommitViewer : No data on database, will clone repo " + commitViewerHelper.getRepoUrl(user, repo)
                    + " and persist data on DB");
            //Create tmp folder
            final File tmpFolder = commitViewerHelper.createTempFolder();
            //Clone repo to tmp folder without checkout (-n flag)
            log.debug("CommitViewer : Cloning repo : " + commitViewerHelper.getRepoUrl(user, repo));
            final List<String> cloneOutput =
                commitViewerHelper.readProcessOutput(tmpFolder, commitViewerHelper.createGitCloneCommand(user, repo), true);

            //check if clone was successful
            if (Files.notExists(Path.of(tmpFolder.getAbsolutePath() + "/" + repo))) {
                log.error("CommitViewer : Failed to clone repo : " + repo);
                //clean tmp folder
                commitViewerHelper.deleteFolder(tmpFolder);

                if (cloneOutput.size() == 3 && cloneOutput.get(1).contains("Repository not found")) {
                    throw new CommitViewerException(ErrorCodeImpl.NOT_FOUND, "Repository not found : " + commitViewerHelper.getRepoUrl(user, repo));
                } else {
                    throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to clone repo");
                }
            } else {
                log.info("CommitViewer : Git repo " + repo + " cloned");
            }

            final File repoFolder = new File(tmpFolder.getAbsolutePath() + "/" + repo);

            //Get total number of commits in repo
            log.debug("CommitViewer : Getting total number of commits");
            totalNumberOfCommits = Integer.parseInt(
                commitViewerHelper.readProcessOutput(repoFolder, List.of("git", "rev-list", "--count", "HEAD"), false).get(0));

            //Make a future call to get all commits
            final long finalTotalNumberOfCommits = totalNumberOfCommits;
            final Future<Boolean> dataStoredFuture = managedExecutor.submit(
                () -> commitViewerHelper.getAllCommitsFromCliAndPersistOnDb(repoFolder, finalTotalNumberOfCommits, user, repo));

            //Get requested commits
            log.debug("CommitViewer : Getting commits...");
            final List<String> logOutput =
                commitViewerHelper.readProcessOutput(repoFolder, commitViewerHelper.createGitLogCommand(false, page, perPage), true);

            //Parse git log response
            log.debug("CommitViewer : Parsing git log response");
            commitInfoDtoList = commitViewerHelper.logOutputToCommitInfoDtoList(logOutput);

            //Get future result
            try {
                dataStoredFuture.get(futureTimeout, TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                log.error("Error waiting for dataStoredFuture result.");
                throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to wait for dataStoredFuture result");
            }

            //clean tmp folder
            FileUtils.deleteDirectory(tmpFolder);
        }

        return CommitInfoPageDto.builder()
                                .items(commitInfoDtoList)
                                .page(page)
                                .perPage(perPage)
                                .count(commitInfoDtoList.size())
                                .total(totalNumberOfCommits)
                                .totalPages((int) Math.ceil((double) totalNumberOfCommits / (double) perPage))
                                .build();
    }
}
