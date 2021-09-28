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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
@Slf4j
public class GitCliService {

    @Inject
    CommitViewerHelper commitViewerHelper;

    public CommitInfoPageDto getCommitList(final String user,
                                           final String repo,
                                           final Integer page,
                                           final Integer perPage) throws IOException, InterruptedException {
        log.info("CommitViewer : Request using GitHub API failed, will use the git CLI as fallback");
        log.debug("CommitViewer : Starting git clone");

        //Create tmp folder
        final File tmpFolder = commitViewerHelper.createTempFolder();
        //Clone repo to tmp folder without checkout (-n flag)
        log.debug("CommitViewer : Cloning repo : " + commitViewerHelper.getRepoUrl(user, repo));
        final List<String> cloneOutput = commitViewerHelper.readProcessOutput(tmpFolder, commitViewerHelper.createGitCloneCommand(user, repo));

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
            log.debug("CommitViewer : git repo " + repo + " cloned");
        }

        //Get commit stats
        log.debug("CommitViewer : Getting commits");
        final File repoFolder = new File(tmpFolder.getAbsolutePath() + "/" + repo);
        final List<String> logOutput = commitViewerHelper.readProcessOutput(repoFolder, commitViewerHelper.createGitLogCommand(page, perPage));
        //parse git log response
        log.debug("CommitViewer : Parsing git log response");
        final List<CommitInfoDto> commitInfoList = commitViewerHelper.logOutputToCommitInfoDtoList(logOutput);
        //get total number of commits in repo
        log.debug("CommitViewer : Getting total number of commits");
        final Integer totalNumberOfCommits = Integer.parseInt(commitViewerHelper.readProcessOutput(repoFolder, List.of("git", "rev-list", "--count", "HEAD")).get(0));

        //clean tmp folder
        FileUtils.deleteDirectory(tmpFolder);

        return CommitInfoPageDto.builder()
                                .items(commitInfoList)
                                .page(page)
                                .perPage(perPage)
                                .count(commitInfoList.size())
                                .total(totalNumberOfCommits)
                                .totalPages((int) Math.ceil((double) totalNumberOfCommits / (double) perPage))
                                .build();
    }
}
