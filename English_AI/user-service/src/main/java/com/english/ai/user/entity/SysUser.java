package com.english.ai.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体 — 映射 sys_user 表
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录账号） */
    private String username;

    /** BCrypt加密密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 考试级别: CET4 / CET6 */
    private String cetLevel;

    /** 手机号 */
    private String phone;

    /** 角色: ROLE_USER / ROLE_ADMIN */
    private String roles;

    /** 状态: 1=正常, 0=禁用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

