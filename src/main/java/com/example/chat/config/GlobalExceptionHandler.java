package com.example.chat.config;

import com.example.chat.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * AI 모델 관련 예외 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ChatResponse> handleAiModelException(RuntimeException e) {
        // 클래스 이름으로 AI 관련 예외 필터링
        if (e.getClass().getName().contains("ollama") || 
            e.getClass().getName().contains("ai")) {
            log.error("AI 모델 에러 발생: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatResponse("AI 모델 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.", "error"));
        }
        // AI 관련 예외가 아니면 일반 예외 핸들러로 위임
        return handleGeneralException(e);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ChatResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 요청: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChatResponse("잘못된 요청입니다: " + e.getMessage(), "error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponse> handleGeneralException(Exception e) {
        log.error("서버 내부 오류: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChatResponse("서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요.", "error"));
    }
} 