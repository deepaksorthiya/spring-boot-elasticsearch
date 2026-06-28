package com.example;

import com.example.config.NativeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(NativeHints.class)
public class ElasticsearchRealWorldApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchRealWorldApplication.class, args);
    }
}
