package com.example.chat.service;

import com.example.chat.config.ChatbotProperties;
import com.example.chat.dto.ChatMessage;
import com.example.chat.dto.ChatResponse;
import com.example.chat.util.classifier.GreetingClassifier;
import com.example.chat.util.classifier.NameQuestionClassifier;
import com.example.chat.util.common.RandomUtil;
import com.example.chat.util.data.JsonUtil;
import com.example.chat.util.normalization.PreprocessingPipeline;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final ChatModel chatModel;
    private final OllamaEmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final SessionMemoryService memoryService;
    private final ChatbotProperties chatbotProperties;
    private final SummaryService summaryService;


    public ChatResponse ask(String sessionId, String question) {
        try {
            memoryService.saveMessage(sessionId, new ChatMessage("user", question));
            question = PreprocessingPipeline.normalize(question);

            if (GreetingClassifier.isSimpleGreeting(question)) {
                return new ChatResponse(RandomUtil.getRandomGreeting(), "stop");
            }

            if (NameQuestionClassifier.isNameQuestion(question)) {
                return new ChatResponse("저는 고객님을 도와드리는 챗봇입니다.", "stop");
            }

            List<String> ragContexts = List.of(); // 초기화

            if (!chatbotProperties.isJustLlm()) {
                try {
                    EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(question), null);
                    EmbeddingResponse embeddingResponse = embeddingModel.call(embeddingRequest);

                    if (!embeddingResponse.getResults().isEmpty()) {
                        var results = vectorStore.similaritySearch(
                                SearchRequest.builder().query(question).topK(5).build()
                        );

                        double threshold = chatbotProperties.getScoreThreshold();

                        ragContexts = Optional.ofNullable(results)
                                .orElse(List.of()) // 결과가 null이면 빈 리스트 처리
                                .stream()
                                .filter(r -> r != null && r.getScore() != null
                                        && r.getScore() >= threshold) // null 체크 + threshold 필터링
                                .map(r -> r.getMetadata().get("answers").toString()) // JSON 문자열 추출
                                .flatMap(json -> JsonUtil.parseAnswers(json).stream()) // JSON 파싱
                                .toList(); // List<String> 생성

                        if (chatbotProperties.isRagOnly()) {
                            if (ragContexts.isEmpty()) {
                                log.info("RAG ONLY 모드: 관련 문서 없음 → 관리자 문의 안내");
                                return new ChatResponse("죄송합니다. 해당 질문은 관리자에게 문의 바랍니다.", "stop");
                            }
                            return new ChatResponse(RandomUtil.getRandomElement(ragContexts), "stop");
                        }
                    }
                } catch (Exception e) {
                    log.warn("RAG 문맥 수집 실패: {}", e.getMessage());
                }
            }

            String summary = memoryService.getSummary(sessionId);
            if (summary == null && memoryService.countMessages(sessionId) >= chatbotProperties.getSummary()
                    .getRecentMessageCount()) {
                summary = summaryService.summarizeRecentMessages(sessionId);
                memoryService.saveSummary(sessionId, summary);
            }

            Prompt prompt = buildPrompt(sessionId, question, summary, ragContexts);

            var response = chatModel.call(prompt);
            String reply = response.getResult().getOutput().getText();
            String finishReason = response.getResult().getMetadata().getFinishReason();

            memoryService.saveMessage(sessionId, new ChatMessage("bot", reply));
            return new ChatResponse(reply, finishReason);

        } catch (Exception e) {
            log.error("처리 중 오류 발생", e);
            return new ChatResponse("죄송합니다. 처리 중 오류가 발생했습니다: " + e.getMessage(), "error");
        }
    }


    /**
     * 프롬프트 문자열을 빌드하여 Prompt 객체로 반환합니다.
     */
    private Prompt buildPrompt(String sessionId, String question, String summary, List<String> ragContexts) {
        StringBuilder promptBuilder = new StringBuilder();

        if (!chatbotProperties.isJustLlm()) {

            promptBuilder.append("[역할]\n");
            promptBuilder.append("너는 ").append(chatbotProperties.getRole()).append(" 챗봇이야.\n\n");

            promptBuilder.append("[필수 사항]\n");
            promptBuilder.append(chatbotProperties.getInstruction()).append("\n\n");
            promptBuilder.append("참고문서에만 근거해서 대답해. 참고문서가 없다면 \"제가 모르는 질문입니다. 관리자에게 문의 부탁드립니다\" 라고 말해.\n");
            promptBuilder.append("거짓으로 지어내서 대답하지 말아.\n\n");

            if (summary != null && !summary.isEmpty()) {
                promptBuilder.append("[이전 요약]\n").append(summary).append("\n\n");
            }

            List<ChatMessage> recent = memoryService.getRecentMessages(sessionId,
                    8);
            promptBuilder.append("[최근 대화]\n");
            for (ChatMessage msg : recent) {
                promptBuilder.append(msg.role()).append(": ").append(msg.content()).append("\n");
            }
            promptBuilder.append("\n");

            promptBuilder.append("[참고 문서]\n");

            if (!ragContexts.isEmpty()) {
                for (String context : ragContexts) {
                    promptBuilder.append("- ").append(context).append("\n");
                }
                promptBuilder.append("\n");
            } else {
                promptBuilder.append("참고 문서는 없습니다.\n\n");
            }
        }

        promptBuilder.append("[질문]\n").append(question);

        log.info("최종 프롬프트 구성: \n{}", promptBuilder);

        return new Prompt(List.of(new UserMessage(promptBuilder.toString())));
    }
}
