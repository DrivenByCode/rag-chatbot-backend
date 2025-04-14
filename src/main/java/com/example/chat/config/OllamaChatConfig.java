package com.example.chat.config;

import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class OllamaChatConfig {
    private final OllamaProperties ollamaProperties;

    @Bean
    @Primary
    public OllamaChatModel ollamaChatModel() {

        // API 객체 생성
        OllamaApi ollamaApi = new OllamaApi(ollamaProperties.getBaseUrl());

        // 옵션 생성
        OllamaOptions options = OllamaOptions.builder()
                .model(ollamaProperties.getChat().getModel())
                .build();

        // 빌더 패턴을 사용하여 OllamaChatModel 생성
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .observationRegistry(ObservationRegistry.NOOP)
                .modelManagementOptions(ModelManagementOptions.builder()
                        .pullModelStrategy(PullModelStrategy.WHEN_MISSING)
                        .build())
                .build();
    }
}