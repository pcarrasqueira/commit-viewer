/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.jpa;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Table(name = "commits")
public class CommitInfoEntity extends PanacheEntityBase {

    @EmbeddedId
    CommitInfoId id;

    @Column(columnDefinition = "TEXT", length = 500)
    private String message;

    private Instant date;

    private String author;

    public static PanacheQuery<CommitInfoEntity> findAllByRepo(final String repo, final Integer page, final Integer perPage) {
        return find("repository = :repo", Sort.by("date").descending(), Parameters.with("repo", repo))
            .page(page - 1, perPage);
    }

    public static long countByRepo(final String repo) {
        return count("repository = :repo", Parameters.with("repo", repo));
    }
}
