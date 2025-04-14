package com.example.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.ollama")
public class OllamaProperties {
    private String baseUrl;
    private Chat chat;
    private Embedding embedding;

    @Data
    public static class Chat {
        private String model;
    }

    @Data
    public static class Embedding {
        private String model;
        private int dimensions;
    }
}