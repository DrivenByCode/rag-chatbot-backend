package com.example.chat.util.common;

import java.util.List;
import java.util.Random;

public class RandomUtil {
    private static final Random RANDOM = new Random();

    /**
     * List에서 랜덤한 요소를 반환합니다.
     *
     * @param list 대상 리스트
     * @param <T>  리스트 요소의 타입
     * @return 랜덤하게 선택된 요소, 리스트가 비어있으면 null 반환
     */
    public static <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

    /**
     * 랜덤한 환영 메시지를 반환합니다.
     *
     * @return 환영 메시지 문자열 중 하나를 랜덤하게 반환
     */
    public static String getRandomGreeting() {
        return getRandomElement(List.of(
                "안녕하세요! 무엇을 도와드릴까요?",
                "반갑습니다. 궁금하신 점이 있으신가요?",
                "안녕하세요! 문의사항이 있으시면 언제든 말씀해 주세요."
        ));
    }
}