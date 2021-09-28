/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubApiCommitDto {

    private String sha;
    private Commit commit;

    @Data
    public static class Commit {
        private Author author;
        private String message;
    }

    @Data
    public static class Author {
        private String name;
        private String email;
        private Instant date;
    }

}
