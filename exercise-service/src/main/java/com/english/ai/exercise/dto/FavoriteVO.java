package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FavoriteVO {
    private Long id;
    private Long userId;
    private String sentenceText;
    private Long questionId;
    private String sourceType;
    private LocalDateTime createdAt;
}
