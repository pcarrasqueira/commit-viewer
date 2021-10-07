/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.service.common;

import com.challenge.model.api.dto.CommitInfoDto;
import com.challenge.model.api.dto.GitHubApiCommitDto;
import com.challenge.model.api.exception.CommitViewerException;
import com.challenge.model.api.exception.ErrorCodeImpl;
import com.challenge.model.api.exception.SkipFallbackException;
import com.challenge.model.jpa.CommitInfoEntity;
import com.challenge.model.jpa.CommitInfoId;
import com.challenge.model.mapper.CommitViewerMapper;
import com.challenge.service.api.GitHubApiClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.client.exception.ResteasyWebApplicationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
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
import java.util.stream.Collectors;

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

    @Inject
    EntityManager em;

    public File createTempFolder() throws IOException {
        final File tmpFile = Files.createTempDirectory("commit-viewer-tmp-dir-").toFile();
        tmpFile.deleteOnExit();
        log.debug("CommitViewer : Created tmp folder at " + tmpFile.getAbsolutePath());
        return tmpFile;
    }

    public List<String> readProcessOutput(final File strTmpDirectory, final List<String> commandArgs, final boolean isGitLog) throws IOException {

        log.debug("CommitViewer : Starting process");
        final List<String> strProcessOutput = new ArrayList<>();
        String line;
        String multiLine = "";

        final ProcessBuilder processBuilder = new ProcessBuilder(commandArgs)
            .redirectErrorStream(true)
            .directory(strTmpDirectory);

        final Process process = processBuilder.start();

        final BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(
                process.getInputStream()));

        while ((line = bufferedReader.readLine()) != null) {
            if (isGitLog) {
                if (line.endsWith("|||") && !line.startsWith("|||") && multiLine.isBlank()) {
                    strProcessOutput.add(line);
                } else if (line.endsWith("|||") && !multiLine.isBlank()) {
                    multiLine = multiLine.isBlank() ? line : multiLine + "\\n" + line;
                    strProcessOutput.add(multiLine);
                    multiLine = "";
                } else {
                    multiLine = multiLine.isBlank() ? line : multiLine + "\\n" + line;
                }
            } else {
                strProcessOutput.add(line);
            }
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

    public List<String> createGitLogCommand(final boolean totalCommits, final Integer page, final Integer perPage) {
        List<String> command = new ArrayList<>();

        if (totalCommits) {
            command = List.of("git", "log", "--pretty=format:%H|||%s|||%b|||%aI|||%an <%ae>|||");
        } else {
            final Integer commitsToSkip = page * perPage - perPage;
            return List.of("git", "log", "--pretty=format:%H|||%s|||%b|||%aI|||%an <%ae>|||", "--skip", commitsToSkip.toString(), "-n", perPage.toString());
        }
        return command;
    }

    //TODO: Need improvement!! If commit message has ||| parse will fail, try to use a mapper ??
    public List<CommitInfoDto> logOutputToCommitInfoDtoList(final List<String> logOutput) {
        final List<CommitInfoDto> commitInfoList = new ArrayList<>();

        try {
            for (String temp : logOutput) {
                final String[] data = temp.split("\\|\\|\\|");
                if (data.length > 5) {
                    log.error("CommitViewer : Failed to parse git log response");
                    throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to parse git log response");
                }
                final String message = data[2].isEmpty() ? data[1] : data[1] + "\\n\\n" + data[2];
                commitInfoList.add(CommitInfoDto.of(data[0],
                                                    message,
                                                    Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(data[3])),
                                                    data[4]));
            }
        } catch (Exception ex) {
            throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to map git log response to CommitInfoDtoList");
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

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public List<CommitInfoDto> getAllCommitsFromApiAndPersistOnDb(final Integer totalPages, final String user,
                                                                  final String repo, final Integer page,
                                                                  final Integer perPage) {

        List<GitHubApiCommitDto> gitHubApiCommitList;
        List<CommitInfoDto> commitInfoDtoList = new ArrayList<>();

        for (int apiCall = 1; apiCall <= totalPages; apiCall++) {
            //Call GitHub API
            try {
                log.debug("CommitViewer : Calling GitHub API");
                gitHubApiCommitList = gitHubApiClient.getCommitsDto(user, repo, apiCall, perPage);
            } catch (ResteasyWebApplicationException e) {
                log.error("CommitViewer : Error retrieving commits from GitHub API.");
                if (e.unwrap().getResponse().getStatus() == 404) {
                    throw new SkipFallbackException(ErrorCodeImpl.NOT_FOUND, "Repository not found : " + getRepoUrl(user, repo));
                } else if (e.unwrap().getResponse().getStatus() == 403 && e.getMessage().contains("rate limit exceeded")) {
                    log.error("CommitViewer : Error retrieving commits from GitHub API.");
                    throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "GitHub API rate limit exceeded");
                } else {
                    throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR, "Failed to contact GitHub API");
                }
            }

            if (apiCall == page) {
                commitInfoDtoList = commitViewerMapper.gitHubApiCommitDtoListToCommitInfoDtoList(gitHubApiCommitList);
            }

            //Persist data on database
            for (GitHubApiCommitDto apiCommit : gitHubApiCommitList) {
                final CommitInfoEntity commitEntry = CommitInfoEntity.of(CommitInfoId.of(apiCommit.getSha(), getRepoUrl(user, repo)),
                                                                         apiCommit.getCommit().getMessage(),
                                                                         apiCommit.getCommit().getAuthor().getDate(),
                                                                         apiCommit.getCommit().getAuthor().getName() + " <" + apiCommit.getCommit().getAuthor().getEmail() + ">");
                commitEntry.persist();
            }
        }
        return commitInfoDtoList;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Boolean getAllCommitsFromCliAndPersistOnDb(final File repoFolder, final long totalNumberOfCommits,
                                                                  final String user, final String repo) {

        try {
            //Get commit stats
            log.debug("CommitViewer : Getting all commits...");
            final List<String> logOutput = readProcessOutput(repoFolder, createGitLogCommand(true, 0, 0), true);
            //Parse git log response
            log.debug("CommitViewer : Parsing git log response");
            final List<CommitInfoDto> commitInfoList = logOutputToCommitInfoDtoList(logOutput);

            if (totalNumberOfCommits != commitInfoList.size()) {
                throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR,
                                                "Total number of commits does not match parsed git log response!");
            }

            //Persist data on database
            int index = 0;
            for (CommitInfoDto commitInfo : commitInfoList) {
                final CommitInfoEntity commitInfoEntity =
                    CommitInfoEntity.of(CommitInfoId.of(commitInfo.getSha(), getRepoUrl(user, repo)),
                                        commitInfo.getMessage(),
                                        commitInfo.getDate(),
                                        commitInfo.getAuthor());
                commitInfoEntity.persist();

                //flush entity manager between batchs
                if (index > 49) {
                    em.flush();
                    em.clear();
                    index = 0;
                }
                index++;
            }
        } catch (Exception ex) {
            throw new CommitViewerException(ErrorCodeImpl.UNKNOWN_ERROR,
                                            "Failed to get all commits and save to DB.");
        }
        return true;
    }

    @Transactional
    public long getTotalNumberOfCommitsFromDb(final String user, final String repo) {
        return CommitInfoEntity.countByRepo(getRepoUrl(user, repo));
    }

    @Transactional
    public List<CommitInfoDto> getCommitsFromDb(final String user, final String repo,
                                                     final Integer page, final Integer perPage) {
        return CommitInfoEntity.findAllByRepo(getRepoUrl(user, repo), page, perPage)
                        .stream()
                        .map(c -> commitViewerMapper.commitInfoEntityToCommitInfoDto(c))
                        .collect(Collectors.toList());
    }

}
