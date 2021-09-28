/*
 * Copyright (c) 2021.
 * Paulo Carrasqueira
 */

package com.challenge.config;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;

import javax.enterprise.inject.Produces;

public class RestAssuredConfigProducer {
    @Produces
    public RestAssuredConfig getRestAssuredConfig() {
        return RestAssured.config = RestAssured.config()
                                               .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());

    }
}
