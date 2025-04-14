package com.example.chat.service;

import com.example.chat.config.ChatbotProperties;
import com.example.chat.dto.ChatMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {
    private final ChatModel chatmodel;
    private final SessionMemoryService memoryService;
    private final ChatbotProperties chatbotProperties;

    /**
     * 세션의 최근 대화 내용을 요약합니다. application.yml에 설정된 recentMessageCount 개수만큼의 메시지를 요약합니다.
     *
     * @param sessionId 세션 ID
     * @return 요약된 텍스트
     */
    public String summarizeRecentMessages(String sessionId) {
        List<ChatMessage> recentMessages = memoryService.getRecentMessages(sessionId,
                chatbotProperties.getSummary().getRecentMessageCount());

        if (recentMessages.isEmpty()) {
            return "대화 내역이 없습니다.";
        }

        log.debug("세션 {}의 최근 {} 개 메시지 요약 시작", sessionId, chatbotProperties.getSummary().getRecentMessageCount());

        String conversationText = recentMessages.stream()
                .map(msg -> msg.role() + ": " + msg.content())
                .collect(Collectors.joining("\n"));

        String prompt = String.format(
                "다음은 사용자의 질문 내용입니다. 이 대화를 간결하게 요약해주세요:\n\n%s\n\n요약:",
                conversationText);

        String summary = chatmodel.call(prompt);
        memoryService.saveSummary(sessionId, summary);

        log.debug("세션 {}의 대화 요약 완료: {}", sessionId, summary);
        return summary;
    }

    /**
     * 저장된 요약을 가져옵니다. 저장된 요약이 없는 경우 새로 요약합니다.
     *
     * @param sessionId 세션 ID
     * @return 요약 텍스트
     */
    public String getSummary(String sessionId) {
        String storedSummary = memoryService.getSummary(sessionId);
        if (storedSummary != null && !storedSummary.isEmpty()) {
            return storedSummary;
        }
        return summarizeRecentMessages(sessionId);
    }
}