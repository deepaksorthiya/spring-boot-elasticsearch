package com.example;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.example.config.NativeHints;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.io.IOException;

import static com.example.constant.Constants.*;

@SpringBootApplication
@ImportRuntimeHints(NativeHints.class)
public class ElasticsearchRealWorldApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchRealWorldApplication.class, args);
    }
}
