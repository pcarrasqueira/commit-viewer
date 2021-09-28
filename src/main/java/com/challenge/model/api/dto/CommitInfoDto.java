/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommitInfoDto {

    @JsonProperty("sha")
    @Schema(
        description = "The commit SHA.",
        example = "b1c714755be333a60ea2167e9ad10d880ab725fc"
    )
    private String sha;

    @JsonProperty("message")
    @Schema(
        description = "The commit message.",
        example = "First commit"
    )
    private String message;

    @JsonProperty("date")
    @Schema(
        description = "The author's commit date.",
        example = "2021-09-25T20:00:00.000Z"
    )
    private Instant date;

    @JsonProperty("author")
    @Schema(
        description = "The author's name and email.",
        example = "John Doe <john.doe@doeemail.com>"
    )
    private String author;
}
