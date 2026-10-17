package com.english.ai.ai.dto;

import lombok.Data;

@Data
public class JudgeFeedbackRequest {
    private String userAnswer;
    private String correctAnswer;
    private String context;
    private Long questionId;
}
