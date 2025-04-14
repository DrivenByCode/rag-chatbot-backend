package com.example.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.vectorstore.qdrant")
public class QdrantProperties {
    private String host;
    private int port;
    private boolean useSsl;
    private String collectionName;
}