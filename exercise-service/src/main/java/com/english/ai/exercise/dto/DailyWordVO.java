package com.english.ai.exercise.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyWordVO {
    private Long id;
    private String word;
    private String phonetic;
    private String translation;
    private String pos;
    private String example;
    private String exampleTranslation;
    private String level;
    private LocalDate date;
}
