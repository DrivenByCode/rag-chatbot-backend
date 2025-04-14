package com.example.chat.util.classifier;

import com.example.chat.util.normalization.SlangNormalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GreetingClassifier {

    // FAQ 파일에 정의된 인사 질문과 유사한 문구를 미리 정의합니다.
    private static final Set<String> GREETINGS = Set.of(
            "안녕", "안녕하세요", "반가워요", "하이", "ㅎㅇ", "ㅎ2", "hi"
    );

    /**
     * 주어진 문자열이 간단한 인사 표현인지 확인합니다.
     * 입력된 질문이 null이거나 공백인 경우 false를 반환합니다.
     * 입력된 질문을 정규화(SlangNormalizer 사용)한 후, 단어 수가 2개 이하인 경우에만
     * 미리 정의된 인사 표현(GREETINGS)에 포함되는지 확인합니다.
     *
     * @param question 사용자가 입력한 질문 또는 문장
     * @return 간단한 인사 표현인 경우 true, 그렇지 않으면 false
     */
    public static boolean isSimpleGreeting(String question) {
        if (question == null || question.trim().isEmpty()) return false;

        String normalized = SlangNormalizer.normalize(question.trim());

        // 단어 수 기준으로 판단 (1~2단어면 인사 가능성 있음)
        if (normalized.split("\\s+").length > 2) return false;

        return GREETINGS.contains(normalized);
    }
}