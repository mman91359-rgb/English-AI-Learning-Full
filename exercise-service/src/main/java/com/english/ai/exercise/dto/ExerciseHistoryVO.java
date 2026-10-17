package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExerciseHistoryVO {
    private Long logId;
    private Long questionId;
    private String stem;
    private String userAnswer;
    private String correctAnswer;
    private Integer isCorrect;
    private Integer durationSeconds;
    private Integer moduleType;
    private String questionType;
    private Integer difficultyLevel;
    private LocalDateTime createdAt;
    private String analysis;
}
