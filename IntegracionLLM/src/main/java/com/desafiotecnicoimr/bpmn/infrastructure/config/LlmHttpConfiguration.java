package com.desafiotecnicoimr.bpmn.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class LlmHttpConfiguration {

    @Bean
    RestClient.Builder llmRestClientBuilder(
            @Value("${llm.http.connect-timeout-ms:10000}") long connectTimeoutMillis,
            @Value("${llm.http.read-timeout-ms:120000}") long readTimeoutMillis
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMillis));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMillis));
        return RestClient.builder().requestFactory(requestFactory);
    }
}
