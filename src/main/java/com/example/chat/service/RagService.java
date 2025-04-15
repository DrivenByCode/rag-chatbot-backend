package com.example.chat.service;

import com.example.chat.dto.ChatResponse;

/**
 * RAG(Retrieval-Augmented Generation) 서비스 인터페이스
 */
public interface RagService {
    /**
     * 사용자의 질문에 대한 응답을 생성합니다.
     *
     * @param sessionId 사용자 세션 ID
     * @param question  사용자 질문
     * @return 챗봇 응답
     */
    ChatResponse ask(String sessionId, String question);
}
