package com.english.ai.user.service;

import com.english.ai.user.dto.LoginRequest;
import com.english.ai.user.dto.LoginResponse;
import com.english.ai.user.dto.RegisterRequest;
import com.english.ai.user.entity.SysUser;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    Long register(RegisterRequest request);

    /**
     * 用户登录 — 返回JWT token
     */
    LoginResponse login(LoginRequest request);

    /**
     * 根据ID查询用户
     */
    SysUser getById(Long userId);

    /**
     * 根据用户名查询用户
     */
    SysUser getByUsername(String username);

    /**
     * 更新用户头像URL
     */
    void updateAvatarUrl(Long userId, String avatarUrl);

    /**
     * 更新用户昵称
     */
    void updateNickname(Long userId, String nickname);

    /**
     * 修改密码 — 需校验原密码
     * @return 校验失败抛 RuntimeException，成功返回 true
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 绑定/更新手机号
     */
    void updatePhone(Long userId, String phone);
}
