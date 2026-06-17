package com.english.ai.user.dto;

import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterRequest {

    /** 用户名 */
    private String username;

    /** 明文密码 */
    private String password;

    /** 考试级别: CET4 / CET6 */
    private String cetLevel;
}
