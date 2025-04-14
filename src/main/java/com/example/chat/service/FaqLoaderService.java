package com.example.chat.service;

import com.example.chat.dto.FaqItem;
import com.example.chat.util.store.QdrantAdminUtil;
import com.example.chat.util.normalization.TextPreprocessorUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaqLoaderService {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final QdrantAdminUtil QdrantAdminUtil;

    // FAQ 관련 상수
    private static final String FAQ_TYPE = "FAQ";
    private static final String FAQ_FILTER_EXPRESSION = "type == '" + FAQ_TYPE + "'";
    private static final String FAQ_FILE_PATH = "data/faqs.json";

    /**
     * FAQ 데이터를 삭제(이미 저장된 FAQ 문서를 삭제)하고 새로 로드하여 인덱싱합니다.
     */
    public boolean loadAndIndexFaqs() {
        try {
            // 기존 FAQ 문서 삭제
            try {
                List<Document> existingDocs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("")
                                .filterExpression(FAQ_FILTER_EXPRESSION)
                                .build()
                );
                if (existingDocs != null && !existingDocs.isEmpty()) {
                    List<String> ids = existingDocs.stream()
                            .map(Document::getId)
                            .collect(Collectors.toList());
                    vectorStore.delete(ids);
                    log.info("기존 FAQ 문서 {}개를 삭제했습니다.", ids.size());
                } else {
                    log.info("삭제할 FAQ 문서가 없습니다.");
                }
            } catch (Exception e) {
                log.warn("FAQ 문서 삭제 중 오류 발생 (컬렉션이 없을 수 있음): {}", e.getMessage());
                // 컬렉션이 없으면 DataInitializer에서 resetCollection()이 처리됨
            }

            // FAQ 항목 로드 및 인덱싱
            List<FaqItem> faqItems = loadFaqsFromJson();
            List<Document> documents = convertFaqsToDocuments(faqItems);

            vectorStore.add(documents);
            log.info("FAQ 데이터 {}개를 성공적으로 로드 및 인덱싱했습니다.", faqItems.size());
            return true;
        } catch (Exception e) {
            log.error("FAQ 데이터 로드 및 인덱싱 중 오류 발생", e);
            return false;
        }
    }

    /**
     * FAQ 데이터를 업데이트합니다. (기존 항목은 유지하고 새 항목만 추가)
     */
    public boolean updateFaqs() {
        try {
            Set<String> existingQuestions = findExistingQuestions();
            List<FaqItem> allFaqs = loadFaqsFromJson();

            // 기존 질문 목록에 포함되지 않은 모든 질문이 포함된 FaqItem만 새로 추가
            List<FaqItem> newFaqs = allFaqs.stream()
                    .filter(faq -> faq.getQuestions().stream()
                            .map(TextPreprocessorUtil::preprocessKorean)
                            .anyMatch(q -> !existingQuestions.contains(q)))
                    .collect(Collectors.toList());

            if (newFaqs.isEmpty()) {
                log.info("새로운 FAQ 항목이 없습니다.");
                return true;
            }

            List<Document> documents = convertFaqsToDocuments(newFaqs);
            vectorStore.add(documents);
            log.info("새로운 FAQ 데이터 {}개를 성공적으로 업데이트했습니다.", newFaqs.size());
            return true;
        } catch (Exception e) {
            log.error("FAQ 데이터 업데이트 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 이미 저장된 FAQ 질문 목록을 검색합니다.
     */
    private Set<String> findExistingQuestions() {
        try {
            List<Document> existingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("")
                            .filterExpression(FAQ_FILTER_EXPRESSION)
                            .build()
            );
            return existingDocs.stream()
                    .map(doc -> doc.getMetadata().getOrDefault("question", "").toString())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("FAQ 질문 목록 검색 중 오류 발생: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * JSON 파일에서 FAQ 항목을 로드합니다.
     */
    private List<FaqItem> loadFaqsFromJson() throws IOException {
        try (InputStream inputStream = new ClassPathResource(FAQ_FILE_PATH).getInputStream()) {
            // JSON 파일 구조에 따라 역직렬화 방법을 조정
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("FAQ JSON 파일 로드 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * FAQ 항목을 Document 객체로 변환합니다.
     */
    private List<Document> convertFaqsToDocuments(List<FaqItem> faqItems) {
        List<Document> documents = new ArrayList<>();

        for (FaqItem item : faqItems) {
            List<String> questions = item.getQuestions();
            List<String> answers = item.getAnswers();

            for (String question : questions) {
                // 첫 번째 답변을 Document의 content로 사용
                String content = item.getAnswer();

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", FAQ_TYPE);
                String preprocessed = TextPreprocessorUtil.preprocessKorean(question);

                metadata.put("question", TextPreprocessorUtil.preprocessKorean(question));

                // List<String> 타입의 answers를 JSON 문자열로 변환하여 저장
                try {
                    String answersJson = objectMapper.writeValueAsString(answers);
                    metadata.put("answers", answersJson);
                } catch (Exception e) {
                    log.error("answers 직렬화 실패", e);
                    metadata.put("answers", "[]");
                }
                metadata.put("category", item.getCategory());

                String id = TextPreprocessorUtil.generateHashId(preprocessed); // 고정된 짧은 ID 생성

                Document doc = new Document(id, content, metadata);  // ID 지정

                documents.add(doc);
            }
        }
        return documents;
    }


    /**
     * FAQ 문서의 총 개수를 계산하고 반환한다.
     * QdrantAdminUtil의 countPoints 메서드를 호출하여 FAQ 문서 수를 가져오며,
     * 오류가 발생할 경우 경고 로그를 출력하고 0을 반환한다.
     *
     * @return FAQ 문서 총 개수를 나타내는 long 값. 오류 발생 시 0을 반환한다.
     */
    public long countFaqDocuments() {
        try {
            return QdrantAdminUtil.countPoints();
        } catch (Exception e) {
            log.warn("FAQ 문서 수 계산 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 카테고리별 FAQ 목록을 반환합니다.
     */
    public Map<String, List<FaqItem>> getFaqsByCategory() {
        try {
            // 1. 벡터스토어에서 모든 FAQ 문서 조회
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("")
                            .filterExpression(FAQ_FILTER_EXPRESSION)
                            .build()
            );

            // 2. 질문과 답변을 FaqItem으로 구성하기 위한 맵 생성
            Map<String, FaqItem> itemsByAnswerKey = new HashMap<>();

            for (Document doc : docs) {
                Map<String, Object> metadata = doc.getMetadata();
                String question = metadata.getOrDefault("question", "").toString();
                String category = metadata.getOrDefault("category", "기타").toString();

                // answers 배열 가져오기
                List<String> answers;
                Object answersObj = metadata.get("answers");
                if (answersObj instanceof List) {
                    answers = (List<String>) answersObj;
                } else {
                    // 이전 버전과의 호환성을 위해 단일 answer도 처리
                    String answer = metadata.getOrDefault("answer", "").toString();
                    answers = Collections.singletonList(answer);
                }

                // 첫 번째 답변과 카테고리로 키를 생성하여 동일한 FAQ 그룹화
                String firstAnswer = answers.isEmpty() ? "" : answers.get(0);
                String key = firstAnswer + "::" + category;

                FaqItem item = itemsByAnswerKey.computeIfAbsent(key, k -> {
                    FaqItem newItem = new FaqItem();
                    newItem.setQuestions(new ArrayList<>());
                    newItem.setAnswers(answers);
                    newItem.setCategory(category);
                    return newItem;
                });

                // 중복되지 않게 질문 추가
                if (!item.getQuestions().contains(question)) {
                    item.getQuestions().add(question);
                }
            }

            // 3. category 기준으로 다시 정리
            Map<String, List<FaqItem>> faqsByCategory = new HashMap<>();
            for (FaqItem item : itemsByAnswerKey.values()) {
                faqsByCategory
                        .computeIfAbsent(item.getCategory(), k -> new ArrayList<>())
                        .add(item);
            }

            return faqsByCategory;
        } catch (Exception e) {
            log.error("카테고리별 FAQ 목록 조회 중 오류 발생", e);
            return Collections.emptyMap();
        }
    }
}
