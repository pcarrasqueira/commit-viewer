/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
@Builder
public class CommitInfoPageDto {

    @JsonProperty("items")
    @Schema(
        description = "The list of commits.",
        type = SchemaType.ARRAY
    )
    private List<CommitInfoDto> items;

    @JsonProperty("page")
    @Schema(
        description = "The requested page.",
        example = "1"
    )
    private int page;

    @JsonProperty("per_page")
    @Schema(
        description = "The requested commits per page.",
        example = "50",
        name = "per_page"
    )
    private int perPage;

    @JsonProperty("count")
    @Schema(
        description = "Total page commits count.",
        example = "33"
    )
    private int count;

    @JsonProperty("total")
    @Schema(
        description = "Total number of commits.",
        example = "123"
    )
    private long total;

    @JsonProperty("total_pages")
    @Schema(
        description = "Total pages count.",
        example = "3",
        name = "total_pages"
    )
    private int totalPages;
}
