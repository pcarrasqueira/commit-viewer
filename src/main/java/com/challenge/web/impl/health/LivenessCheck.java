/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.web.impl.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;

@Liveness
@ApplicationScoped
public class LivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named(LivenessCheck.class.getSimpleName()).up().build();
    }
}
