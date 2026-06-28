package com.example.config;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;

public class TestConfiguration {

    private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.18.5";

    @Container
    @ServiceConnection
    static ElasticsearchContainer esContainer = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            .withEnv("ES_JAVA_OPTS", "-Xms500m -Xmx500m")
            .withEnv("path.repo", "/tmp") // for snapshots
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false")
            .withStartupTimeout(Duration.ofSeconds(60));

    @DynamicPropertySource
    static void envProperties(DynamicPropertyRegistry registry) {
        // add custom env prop here
        //registry.add("spring.elasticsearch.uris", esContainer::getHttpHostAddress);
        System.out.println(esContainer);
    }

}
