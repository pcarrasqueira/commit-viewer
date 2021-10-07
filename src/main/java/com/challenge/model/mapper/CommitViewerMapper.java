/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.mapper;

import com.challenge.model.api.dto.CommitInfoDto;
import com.challenge.model.api.dto.GitHubApiCommitDto;
import com.challenge.model.jpa.CommitInfoEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import javax.inject.Inject;
import java.util.List;

@Mapper(componentModel = "cdi")
public abstract class CommitViewerMapper {

    @Inject
    ObjectMapper objectMapper;

    public List<GitHubApiCommitDto> gitHubApiResponseToGitHubApiCommitDto(final String response)
        throws JsonProcessingException {
        return objectMapper.readValue(response, new TypeReference<List<GitHubApiCommitDto>>() { });
    }

    //Map GitHub API response to our CommitInfoDto
    @Mapping(target = "message", source = "commit.message")
    @Mapping(target = "date", source = "commit.author.date")
    @Mapping(target = "author", expression = "java(gitHubApiCommitDto.getCommit().getAuthor().getName() + \" <\" +  gitHubApiCommitDto.getCommit().getAuthor"
                                             + "().getEmail() + \">\")")
    @Named("gitHubApiCommitDtoToCommitInfoDto")
    public abstract CommitInfoDto gitHubApiCommitDtoToCommitInfoDto(GitHubApiCommitDto gitHubApiCommitDto);

    //Iterable Mapper to get List of CommitInfoDto from a List of GitHubApiCommitDto
    @IterableMapping(qualifiedByName = "gitHubApiCommitDtoToCommitInfoDto")
    public abstract List<CommitInfoDto> gitHubApiCommitDtoListToCommitInfoDtoList(List<GitHubApiCommitDto> gitHubApiCommit);

    //Map CommitInfoEntity to CommitInfoDto
    @Mapping(target = "sha", source = "id.sha")
    public abstract CommitInfoDto commitInfoEntityToCommitInfoDto(CommitInfoEntity commitInfoEntity);
}
