package com.manutd.analyst.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FootballClientConfig {

    @Bean
    RestClient footballRestClient(
            @Value("${football.api.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
