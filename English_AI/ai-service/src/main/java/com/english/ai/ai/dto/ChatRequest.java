package com.english.ai.ai.dto;

import lombok.Data;

@Data
public class ChatRequest {
    /** 用户消息 */
    private String message;
    /** 上下文类型：vocab(词汇), grammar(语法), general(通用) */
    private String contextType;
    /** 当前题目内容（可选） */
    private String questionContent;
    /** 用户答案（可选） */
    private String userAnswer;
    /** 对话历史（可选） */
    private java.util.List<ChatMessage> history;
    /** 评分类型：speaking(口语), writing(作文)（可选） */
    private String scoreType;

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
