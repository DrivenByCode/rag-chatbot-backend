package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaqItem {
    private List<String> questions = new ArrayList<>();
    private List<String> answers = new ArrayList<>();
    private String category;

    // 랜덤 답변 가져오기
    public String getRandomAnswer() {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return answers.get(random.nextInt(answers.size()));
    }

    // 첫 번째 답변 반환
    public String getAnswer() {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        return answers.get(0);
    }
}
