package com.english.ai.exercise.dto;

import lombok.Data;

@Data
public class AddFavoriteRequest {
    private Long userId;
    private String sentenceText;
    private Long questionId;
}
