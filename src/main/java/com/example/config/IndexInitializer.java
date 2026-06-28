package com.example.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.constant.Constants.*;

@Component
public class IndexInitializer implements ApplicationRunner {

    private final ElasticsearchClient elasticRestClient;

    public IndexInitializer(ElasticsearchClient elasticRestClient) {
        this.elasticRestClient = elasticRestClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Creating the indexes
        createSimpleIndex(elasticRestClient, USERS.getName());
        createIndexWithDateMapping(elasticRestClient, ARTICLES.getName());
        createIndexWithDateMapping(elasticRestClient, COMMENTS.getName());
    }

    private void createSimpleIndex(ElasticsearchClient esClient, String index) throws IOException {
        BooleanResponse indexRes = esClient.indices().exists(ex -> ex.index(index));
        if (!indexRes.value()) {
            esClient.indices().create(c -> c.index(index));
        }
    }

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
