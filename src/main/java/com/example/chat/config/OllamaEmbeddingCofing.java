package com.example.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class OllamaEmbeddingCofing {
    private final OllamaProperties ollamaProperties;

    @Bean
    @Primary
    public OllamaEmbeddingModel ollamaEmbeddingModel() {

        // API 객체 생성
        OllamaApi ollamaApi = new OllamaApi(ollamaProperties.getBaseUrl());

        // 옵션 생성
        OllamaOptions options = OllamaOptions.builder()
                .model(ollamaProperties.getEmbedding().getModel())
                .build();

        // 빌더 패턴을 사용하여 OllamaEmbedding 생성
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }
}