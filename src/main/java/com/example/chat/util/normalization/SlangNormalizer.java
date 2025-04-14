package com.example.chat.util.normalization;

import java.util.HashMap;
import java.util.Map;

public class SlangNormalizer {

    private static final Map<String, String> SLANG_MAP = new HashMap<>();

    static {
        SLANG_MAP.put("ㅎㅇ", "안녕");
        SLANG_MAP.put("ㅎ2", "안녕");
        SLANG_MAP.put("ㅂㅂ", "그만해줘");
        SLANG_MAP.put("ㄱㄱ", "그래");
        SLANG_MAP.put("ㄴㄴ", "아니야");
        // 필요한 만큼 추가
    }

    public static String normalize(String input) {
        return SLANG_MAP.getOrDefault(input, input);
    }
}
