package com.example;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

import static com.example.constant.Constants.*;

@SpringBootApplication
public class ElasticsearchRealWorldApplication implements ApplicationRunner {

    private final ElasticsearchClient elasticRestClient;

    public ElasticsearchRealWorldApplication(ElasticsearchClient elasticRestClient) {
        this.elasticRestClient = elasticRestClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchRealWorldApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Creating the indexes
        createSimpleIndex(elasticRestClient, USERS.getName());
        createIndexWithDateMapping(elasticRestClient, ARTICLES.getName());
        createIndexWithDateMapping(elasticRestClient, COMMENTS.getName());
    }

    /**
     * Plain simple
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html">index</a>
     * creation with an
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-exists.html">
     * exists</a> check
     */
    private void createSimpleIndex(ElasticsearchClient esClient, String index) throws IOException {
        BooleanResponse indexRes = esClient.indices().exists(ex -> ex.index(index));
        if (!indexRes.value()) {
            esClient.indices().create(c -> c
                    .index(index));
        }
    }

    /**
     * If no explicit mapping is defined, elasticsearch will dynamically map types when converting data to
     * the json
     * format. Adding explicit mapping to the date fields assures that no precision will be lost. More
     * information about
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-field-mapping.html">dynamic
     * field mapping</a>, more on <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-date-format
     * .html">mapping date
     * format </a>
     */
    private void createIndexWithDateMapping(ElasticsearchClient esClient, String index) throws IOException {
        BooleanResponse indexRes = esClient.indices().exists(ex -> ex.index(index));
        if (!indexRes.value()) {
            esClient.indices().create(c -> c
                    .index(index)
                    .mappings(m -> m
                            .properties("createdAt", p -> p
                                    .date(d -> d.format("strict_date_optional_time")))
                            .properties("updatedAt", p -> p
                                    .date(d -> d.format("strict_date_optional_time")))));

        }
    }
}
