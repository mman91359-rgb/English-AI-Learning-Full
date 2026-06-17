package com.english.ai.user.controller;

import com.english.ai.common.Result;
import com.english.ai.user.dto.AssessSubmitRequest;
import com.english.ai.user.dto.LoginRequest;
import com.english.ai.user.dto.LoginResponse;
import com.english.ai.user.dto.RegisterRequest;
import com.english.ai.user.entity.SysUser;
import com.english.ai.user.service.UserAbilityRadarService;
import com.english.ai.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserAbilityRadarService radarService;

    // ========== 注册/登录 ==========

    @PostMapping("/register")
    public Result<Long> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return Result.badRequest("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return Result.badRequest("密码不能少于6位");
        }
        try {
            Long userId = userService.register(request);
            log.info("注册接口调用成功: userId={}", userId);
            return Result.success(userId);
        } catch (RuntimeException e) {
            log.warn("注册失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.conflict(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return Result.badRequest("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return Result.badRequest("密码不能为空");
        }
        try {
            LoginResponse response = userService.login(request);
            log.info("登录接口调用成功: username={}", request.getUsername());
            return Result.success(response);
        } catch (RuntimeException e) {
            log.warn("登录失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.unauthorized(e.getMessage());
        }
    }

    // ========== 用户查询 ==========

    @GetMapping("/info/{userId}")
    public Result<SysUser> info(@PathVariable Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @GetMapping("/by-username/{username}")
    public Result<SysUser> byUsername(@PathVariable String username) {
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    // ========== 能力雷达 / ELO ==========

    /**
     * 获取能力雷达图数据
     * GET /user/radar/{userId}
     */
    @GetMapping("/radar/{userId}")
    public Result<Map<String, Object>> radar(@PathVariable Long userId) {
        Map<String, Object> data = radarService.getRadarData(userId);
        return Result.success(data);
    }

    /**
     * 更新ELO分（由exercise-service在答题后调用）
     * POST /user/radar/update-elo
     * Body: {"userId": 1, "difficultyLevel": 3, "moduleType": 1, "isCorrect": true}
     */
    @PostMapping("/radar/update-elo")
    public Result<Map<String, Object>> updateElo(@RequestBody AssessSubmitRequest request) {
        if (request.getUserId() == null || request.getDifficultyLevel() == null) {
            return Result.badRequest("userId和difficultyLevel不能为空");
        }
        int newElo = radarService.updateElo(
                request.getUserId(),
                request.getDifficultyLevel(),
                Boolean.TRUE.equals(request.getIsCorrect()),
                false);
        Map<String, Object> data = radarService.getRadarData(request.getUserId());
        log.info("ELO更新: userId={}, newElo={}", request.getUserId(), newElo);
        return Result.success(data);
    }

    /**
     * 提交定级评估题（实时更新ELO，K=40快速收敛）
     * POST /user/assess/submit
     * Body: {"userId": 1, "difficultyLevel": 3, "moduleType": 1, "isCorrect": true}
     */
    @PostMapping("/assess/submit")
    public Result<Map<String, Object>> assessSubmit(@RequestBody AssessSubmitRequest request) {
        if (request.getUserId() == null || request.getDifficultyLevel() == null) {
            return Result.badRequest("userId和difficultyLevel不能为空");
        }
        int newElo = radarService.updateElo(
                request.getUserId(),
                request.getDifficultyLevel(),
                Boolean.TRUE.equals(request.getIsCorrect()),
                true);
        Map<String, Object> data = radarService.getRadarData(request.getUserId());
        log.info("定级评估: userId={}, difficulty={}, correct={}, newElo={}",
                request.getUserId(), request.getDifficultyLevel(), request.getIsCorrect(), newElo);
        return Result.success(data);
    }
}
