package com.example.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chatbot")
public class ChatbotProperties {
    private String role;
    private String instruction;
    private double scoreThreshold;
    private boolean justLlm;
    private boolean ragOnly;
    private Summary summary;

    @Data
    public static class Summary {
        private int recentMessageCount;
    }
}