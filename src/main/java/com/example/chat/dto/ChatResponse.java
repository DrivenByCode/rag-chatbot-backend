package com.example.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 챗봇 응답을 위한 DTO 클래스
 */
public record ChatResponse(
        String reply,
        String finishReason,
        LocalDateTime timestamp,
        List<String> references,
        Integer tokenCount
) {
    /**
     * 기본 생성자 - 호환성 유지를 위한 오버로딩
     */
    public ChatResponse(String reply, String finishReason) {
        this(reply, finishReason, LocalDateTime.now(), List.of(), null);
    }

    /**
     * 참조 문서가 있는 응답 생성
     */
    public static ChatResponse withReferences(String reply, String finishReason, List<String> references) {
        return new ChatResponse(reply, finishReason, LocalDateTime.now(), references, null);
    }

}