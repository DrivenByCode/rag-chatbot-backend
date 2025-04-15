package com.example.chat.config;

import com.example.chat.service.FaqLoaderService;
import com.example.chat.util.store.QdrantAdminUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final FaqLoaderService faqLoaderService;
    private final QdrantAdminUtil qdrantAdminUtil;

    @PostConstruct
    public void initializeData() {
        log.info("Qdrant 컬렉션 초기화 시작...");
        qdrantAdminUtil.resetCollection();
        log.info("Qdrant 컬렉션 초기화 완료");

        log.info("FAQ 데이터 로드 시작...");
        faqLoaderService.loadAndIndexFaqs();
    }

}