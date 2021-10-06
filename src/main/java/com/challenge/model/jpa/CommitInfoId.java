/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import java.io.Serializable;


@Embeddable
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class CommitInfoId implements Serializable {

    private String sha;

    private String repository;
}
