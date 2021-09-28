/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.service.common;

import com.challenge.model.api.dto.CommitInfoDto;
import com.challenge.model.api.exception.CommitViewerException;
import com.challenge.model.api.exception.ErrorCodeImpl;
import com.challenge.model.api.exception.SkipFallbackException;
import com.challenge.model.api.mapper.CommitViewerMapper;
import com.challenge.service.api.GitHubApiClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.client.exception.ResteasyWebApplicationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
@Slf4j
public class CommitViewerHelper {

    @ConfigProperty(name = "commit-viewer.github-url", defaultValue = "https://github.com/")
    String gitHubUrl;

    @Inject
    @RestClient
    GitHubApiClient gitHubApiClient;

    @Inject
    CommitViewerMapper commitViewerMapper;

    public File createTempFolder() throws IOException {
        final File tmpFile = Files.createTempDirectory("commit-viewer-tmp-dir-").toFile();
        tmpFile.deleteOnExit();
        log.debug("CommitViewer : Created tmp folder at " + tmpFile.getAbsolutePath());
        return tmpFile;
    }

    public List<String> readProcessOutput(final File strTmpDirectory, final List<String> commandArgs) throws IOException, InterruptedException {

        log.debug("CommitViewer : Starting process");
        final List<String> strProcessOutput = new ArrayList<>();
        String line;

        final ProcessBuilder processBuilder = new ProcessBuilder(commandArgs)
            .redirectErrorStream(true)
            .directory(strTmpDirectory);

        final Process process = processBuilder.start();

        final BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(
                process.getInputStream()));

        while ((line = bufferedReader.readLine()) != null) {
            strProcessOutput.add(line);
        }

        try {
            process.waitFor();
        } catch (Exception ex) {
            log.error("CommitViewer : Error waiting for process");
            deleteFolder(strTmpDirectory);
            throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to wait for process");
        }
        log.debug("CommitViewer : Process finished");
        return strProcessOutput;
    }

    public List<String> createGitCloneCommand(final String strUser, final String strRepo) {
        return List.of("git", "clone", "-n", getRepoUrl(strUser, strRepo));
    }

    public List<String> createGitLogCommand(final Integer page, final Integer perPage) {
        final Integer commitsToSkip = page * perPage - perPage;
        return List.of("git", "log", "--pretty=format:%H|||%s|||%b|||%aI|||%an <%ae>", "--skip", commitsToSkip.toString(), "-n", perPage.toString());
    }

    //TODO: Need improvement!! If commit message has ||| parse will fail, try to use a mapper ??
    public List<CommitInfoDto> logOutputToCommitInfoDtoList(final List<String> logOutput) {
        final List<CommitInfoDto> commitInfoList = new ArrayList<>();

        for (String temp : logOutput) {
            final String[] data = temp.split("\\|\\|\\|");
            if (data.length > 5) {
                log.error("CommitViewer : Failed to parse git log response");
                throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to parse git log response");
            }
            commitInfoList.add(CommitInfoDto.of(data[0],
                                                data[2].isEmpty() ? data[1] : data[1] + "\\n\\n" + data[2],
                                                Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(data[3])),
                                                data[4]));
        }
        return commitInfoList;
    }

    public Integer getTotalNumberOfCommitsFromLink(final String user, final String repo, final Integer perPage) {

        final Response gitHubApiResponseFirstCall;
        final Response gitHubApiResponseSecondCall;

        int totalPages = 0;
        int totalCommits = 0;
        int firstPageItems = 0;
        int lastPageItems = 0;

        //Make first call to get last link
        try {
            log.debug("CommitViewer : Calling GitHub API");
            gitHubApiResponseFirstCall = gitHubApiClient.getCommitsResponse(user, repo, 1, perPage);
        } catch (ResteasyWebApplicationException e) {
            log.error("CommitViewer : Error retrieving commits from GitHub API.");
            if (e.unwrap().getResponse().getStatus() == 404) {
                throw new SkipFallbackException(ErrorCodeImpl.NOT_FOUND, "Repository not found : " + getRepoUrl(user, repo));
            } else {
                throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to contact GitHub API");
            }
        }

        //Map response body to List of GitHubApiCommitDto and get total number of commits
        try {
            firstPageItems = commitViewerMapper.gitHubApiResponseToGitHubApiCommitDto(
                gitHubApiResponseFirstCall.readEntity(String.class)).size();
        } catch (Exception e) {
            log.error("CommitViewer : Error retrieving commits from GitHub API.", e);
            throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to contact GitHub API");
        }

        //Get last link from headers (thanks GitHub)
        final String lastLink = gitHubApiResponseFirstCall.getLink("last") == null ? "0" : gitHubApiResponseFirstCall.getLink("last").toString();

        //only one page duhhh
        if ("0".equals(lastLink)) {
            totalCommits = firstPageItems;
        } else {
            // We need to get last page from link
            final Pattern pattern = Pattern.compile("\\=(.*?)\\&");
            final Matcher matcher = pattern.matcher(lastLink);

            // now try to find the regex match and get total pages
            if (matcher.find()) {
                totalPages = Integer.parseInt(matcher.group(1));
            } else {
                log.error("CommitViewer : Did not find a match");
            }

            //Make second call to get last page items
            try {
                log.debug("CommitViewer : Calling GitHub API");
                gitHubApiResponseSecondCall = gitHubApiClient.getCommitsResponse(user, repo, totalPages, perPage);
            } catch (ResteasyWebApplicationException e) {
                log.error("CommitViewer : Error retrieving commits from GitHub API.");
                if (e.unwrap().getResponse().getStatus() == 404) {
                    throw new SkipFallbackException(ErrorCodeImpl.NOT_FOUND,
                                                    "Repository not found : " + getRepoUrl(user, repo));
                } else {
                    throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to contact GitHub API");
                }
            }

            //Map response body to List of GitHubApiCommitDto and get total number of commits
            try {
                lastPageItems = commitViewerMapper.gitHubApiResponseToGitHubApiCommitDto(
                    gitHubApiResponseSecondCall.readEntity(String.class)).size();
            } catch (Exception e) {
                log.error("CommitViewer : Error retrieving commits from GitHub API.", e);
                throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to contact GitHub API");
            }
            totalCommits = ((totalPages - 1) * perPage) + lastPageItems;
        }

        return totalCommits;
    }

    public void deleteFolder(final File folder) {
        log.debug("CommitViewer : Deleting folder " + folder.getAbsolutePath());
        try {
            FileUtils.deleteDirectory(folder);
        } catch (Exception ex) {
            throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to delete temp folder " + folder.getAbsolutePath());
        }
    }

    public String getRepoUrl(final String user, final String repo) {
        return gitHubUrl + user + "/" + repo;
    }
}
