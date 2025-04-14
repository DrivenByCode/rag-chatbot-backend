package com.example.chat.util.classifier;

import com.example.chat.util.normalization.TextPreprocessorUtil;

public class NameQuestionClassifier {

    /**
     * 입력된 질문 문자열이 이름에 관한 질문인지 여부를 판별합니다.
     * 질문 문자열에 "이름" 또는 "존함"이 포함되어 있어야 하며,
     * 해당 문자열이 챗봇을 대상으로 하고 있음을 암시하는 단어가 포함된 경우에만 true를 반환합니다.
     * 그렇지 않으면 false를 반환합니다.
     *
     * @param question 판별할 질문 문자열
     * @return 입력된 문자열이 이름에 관한 질문인 경우 true, 그 외의 경우 false
     */
    public static boolean isNameQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return false;
        }

        String processed = TextPreprocessorUtil.preprocessKorean(question.trim());

        // "이름" 또는 "존함"이 포함되어 있고, 대상이 챗봇임을 암시하는 단어가 있을 때만 true
        boolean containsName = processed.contains("이름") || processed.contains("존함");
        boolean refersToBot = processed.contains("너") || processed.contains("뭐");

        // "이름"은 있으나 대상이 명확하지 않으면 false
        return containsName && refersToBot;
    }
}

