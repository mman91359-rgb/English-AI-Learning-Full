package com.english.ai.user.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginResponse {

    /** JWT Token */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 手机号 */
    private String phone;

    /** 考试级别 */
    private String cetLevel;

    /** 角色 */
    private String roles;

    /** ELO隐藏分 */
    private Integer eloScore;

    /** ELO等级名称 */
    private String eloLevel;
}
