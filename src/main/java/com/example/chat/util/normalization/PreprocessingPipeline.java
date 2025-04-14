package com.example.chat.util.normalization;

public class PreprocessingPipeline {

    public static String normalize(String input) {
        if (input == null || input.isBlank()) return "";
        String cleaned = TextPreprocessorUtil.preprocessQuery(input);
        return SlangNormalizer.normalize(cleaned);
    }
}