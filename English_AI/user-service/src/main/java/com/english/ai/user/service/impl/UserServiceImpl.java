package com.english.ai.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.english.ai.common.JwtUtil;
import com.english.ai.common.PasswordUtil;
import com.english.ai.user.dto.LoginRequest;
import com.english.ai.user.dto.LoginResponse;
import com.english.ai.user.dto.RegisterRequest;
import com.english.ai.user.entity.SysUser;
import com.english.ai.user.entity.UserAbilityRadar;
import com.english.ai.user.mapper.UserMapper;
import com.english.ai.user.service.UserAbilityRadarService;
import com.english.ai.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserAbilityRadarService radarService;

    @Override
    public Long register(RegisterRequest request) {
        SysUser exist = getByUsername(request.getUsername());
        if (exist != null) {
            throw new RuntimeException("用户名已存在: " + request.getUsername());
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setCetLevel(request.getCetLevel());
        user.setPhone(request.getPhone());
        user.setRoles("ROLE_USER");
        user.setStatus(1);

        userMapper.insert(user);
        log.info("新用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        // 初始化能力雷达记录，ELO=1000
        radarService.initRadar(user.getId(), request.getCetLevel());

        return user.getId();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = getByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = JwtUtil.generate(user.getId(), user.getUsername(), user.getRoles());

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

        // 查询雷达数据获取ELO
        Integer eloScore = null;
        String eloLevel = null;
        UserAbilityRadar radar = radarService.getByUserId(user.getId());
        if (radar != null) {
            eloScore = radar.getEloScore();
            eloLevel = eloToLevel(radar.getEloScore());
        }

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername(

                ))
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .cetLevel(user.getCetLevel())
                .roles(user.getRoles())
                .eloScore(eloScore)
                .eloLevel(eloLevel)
                .build();
    }

    @Override
    public SysUser getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 更新用户头像URL
     */
    @Override
    public void updateAvatarUrl(Long userId, String avatarUrl) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        log.info("用户头像更新成功: userId={}, avatarUrl={}", userId, avatarUrl);
    }

    /**
     * 更新用户昵称
     */
    @Override
    public void updateNickname(Long userId, String nickname) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setNickname(nickname);
        userMapper.updateById(user);
        log.info("用户昵称更新成功: userId={}, nickname={}", userId, nickname);
    }

    /**
     * 修改密码 — 需校验原密码
     */
    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 校验原密码
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        // 加密新密码并更新
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);
        log.info("用户密码修改成功: userId={}", userId);
        return true;
    }

    /**
     * 绑定/更新手机号
     */
    @Override
    public void updatePhone(Long userId, String phone) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 校验手机号格式（11位数字，1开头）
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        user.setPhone(phone);
        userMapper.updateById(user);
        log.info("用户手机号绑定成功: userId={}, phone={}", userId, phone);
    }

    private String eloToLevel(int elo) {
        if (elo >= 2000) return "宗师";
        if (elo >= 1750) return "大师";
        if (elo >= 1500) return "高手";
        if (elo >= 1250) return "进阶";
        if (elo >= 1000) return "入门";
        return "初心者";
    }
}

