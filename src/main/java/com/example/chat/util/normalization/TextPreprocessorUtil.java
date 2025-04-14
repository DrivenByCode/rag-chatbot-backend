package com.example.chat.util.normalization;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * 텍스트 전처리를 위한 유틸리티 클래스 텍스트 정규화, 한국어 조사 제거 등 다양한 전처리 기능을 제공합니다.
 */
@Slf4j
public class TextPreprocessorUtil {

    /**
     * 입력된 한국어 텍스트를 전처리하는 메서드. 텍스트를 소문자로 변환하고 양쪽 공백을 제거한 뒤, 특정 한국어 조사를 제거하여 반환한다.
     *
     * @param text 전처리를 수행할 한국어 텍스트 입력값
     * @return 전처리된 한국어 텍스트
     */
    public static String preprocessKorean(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 기본 전처리
        text = text.toLowerCase().trim();

//         한국어 조사 제거 (예시)
//        text = text.replaceAll("이\\s", " ").replaceAll("가\\s", " ")
//                .replaceAll("은\\s", " ").replaceAll("는\\s", " ")
//                .replaceAll("을\\s", " ").replaceAll("를\\s", " ");

        return text;
    }

    /**
     * 검색 쿼리를 전처리하는 메서드. 한국어 전처리를 적용하고 연속된 공백을 제거합니다.
     *
     * @param query 전처리할 쿼리 문자열
     * @return 전처리된 쿼리 문자열
     */
    public static String preprocessQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }

        // 한국어 전처리 적용
        String processed = preprocessKorean(query);

        // 연속된 공백을 단일 공백으로 변환
        processed = processed.replaceAll("\\s+", " ");

        log.debug("원본 쿼리: '{}', 전처리 후: '{}'", query, processed);

        return normalizeText(processed);
    }

    /**
     * 복잡한 텍스트 정규화를 위한 메서드. 특수문자 처리, 불필요한 기호 제거 등을 수행합니다.
     *
     * @param text 정규화할 텍스트
     * @return 정규화된 텍스트
     */
    public static String normalizeText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 특수문자 처리 - 기본적인 구두점 유지하면서 특수문자 제거
        text = text.replaceAll("[^\\p{L}\\p{N}\\s.,;:?!'\"()]", " ");

        // 연속된 구두점 정규화
        text = text.replaceAll("[.]{2,}", "...");
        text = text.replaceAll("[!]{2,}", "!");
        text = text.replaceAll("[?]{2,}", "?");

        // 연속된 공백 처리
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    /**
     * 토큰화를 위한 간단한 구현. 텍스트를 공백을 기준으로 분리합니다.
     *
     * @param text 토큰화할 텍스트
     * @return 토큰 배열
     */
    public static String[] tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        // 전처리 수행
        String processed = preprocessKorean(text);

        // 공백 기준 분리
        return processed.split("\\s+");
    }

    /**
     * 주어진 입력 문자열로부터 고유한 해시 ID를 생성합니다. 이 메서드는 UUID.nameUUIDFromBytes를 사용하여 입력 값에 기반한 고유한 문자열을 반환합니다.
     *
     * @param input 해시 ID를 생성할 입력 문자열
     * @return 입력 문자열 기반의 고유한 해시 ID 문자열
     */
    public static String generateHashId(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }
}