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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

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

    /** 头像文件存储目录，从配置文件读取 */
    @Value("${avatar.upload-dir:./uploads/avatars}")
    private String avatarUploadDir;

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

    // ========== 头像上传 ==========

    /**
     * 上传用户头像
     * POST /user/avatar/upload
     * 参数: userId (表单字段) + file (头像文件)
     * 返回: avatarUrl (头像访问路径)
     */
    @PostMapping("/avatar/upload")
    public Result<String> uploadAvatar(
            @RequestParam("userId") Long userId,
            @RequestParam("file") MultipartFile file) {

        // 校验文件
        if (file == null || file.isEmpty()) {
            return Result.badRequest("请选择头像文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.badRequest("只能上传图片文件");
        }
        // 限制文件大小 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.badRequest("头像文件不能超过5MB");
        }

        try {
            // 确保存储目录存在
            Path uploadPath = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // 生成唯一文件名，保留原始扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = userId + "_" + UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件到磁盘
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            // 构建头像访问URL（通过后端静态资源接口访问）
            String avatarUrl = "/user/avatar/" + filename;

            // 更新数据库中的头像URL
            userService.updateAvatarUrl(userId, avatarUrl);

            log.info("头像上传成功: userId={}, avatarUrl={}", userId, avatarUrl);
            return Result.success(avatarUrl);

        } catch (IOException e) {
            log.error("头像上传失败: userId={}", userId, e);
            return Result.error(500, "头像上传失败，请稍后重试");
        }
    }

    // ========== 修改昵称 ==========

    /**
     * 修改用户昵称
     * POST /user/nickname/update
     * Body: {"userId": 1, "nickname": "新昵称"}
     */
    @PostMapping("/nickname/update")
    public Result<String> updateNickname(@RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("userId");
        Object nicknameObj = body.get("nickname");
        if (userIdObj == null) {
            return Result.badRequest("userId不能为空");
        }
        if (nicknameObj == null || String.valueOf(nicknameObj).isBlank()) {
            return Result.badRequest("昵称不能为空");
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));
        String nickname = String.valueOf(nicknameObj).trim();
        // 限制昵称长度
        if (nickname.length() > 20) {
            return Result.badRequest("昵称长度不能超过20个字符");
        }
        try {
            userService.updateNickname(userId, nickname);
            log.info("昵称修改成功: userId={}, nickname={}", userId, nickname);
            return Result.success(nickname);
        } catch (RuntimeException e) {
            log.warn("昵称修改失败: userId={}, reason={}", userId, e.getMessage());
            return Result.error(404, e.getMessage());
        }
    }

    // ========== 修改密码 ==========

    /**
     * 修改用户密码
     * POST /user/password/update
     * Body: {"userId": 1, "oldPassword": "旧密码", "newPassword": "新密码"}
     */
    @PostMapping("/password/update")
    public Result<Boolean> updatePassword(@RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("userId");
        Object oldPwdObj = body.get("oldPassword");
        Object newPwdObj = body.get("newPassword");
        if (userIdObj == null) {
            return Result.badRequest("userId不能为空");
        }
        if (oldPwdObj == null || String.valueOf(oldPwdObj).isBlank()) {
            return Result.badRequest("原密码不能为空");
        }
        if (newPwdObj == null || String.valueOf(newPwdObj).length() < 6) {
            return Result.badRequest("新密码不能少于6位");
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));
        String oldPassword = String.valueOf(oldPwdObj);
        String newPassword = String.valueOf(newPwdObj);
        // 新旧密码不能相同
        if (oldPassword.equals(newPassword)) {
            return Result.badRequest("新密码不能与原密码相同");
        }
        try {
            userService.updatePassword(userId, oldPassword, newPassword);
            log.info("密码修改成功: userId={}", userId);
            return Result.success(true);
        } catch (RuntimeException e) {
            log.warn("密码修改失败: userId={}, reason={}", userId, e.getMessage());
            return Result.badRequest(e.getMessage());
        }
    }

    // ========== 绑定手机号 ==========

    /**
     * 绑定/更新用户手机号
     * POST /user/phone/update
     * Body: {"userId": 1, "phone": "13800138000"}
     */
    @PostMapping("/phone/update")
    public Result<String> updatePhone(@RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("userId");
        Object phoneObj = body.get("phone");
        if (userIdObj == null) {
            return Result.badRequest("userId不能为空");
        }
        if (phoneObj == null || String.valueOf(phoneObj).isBlank()) {
            return Result.badRequest("手机号不能为空");
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));
        String phone = String.valueOf(phoneObj).trim();
        try {
            userService.updatePhone(userId, phone);
            log.info("手机号绑定成功: userId={}, phone={}", userId, phone);
            return Result.success(phone);
        } catch (RuntimeException e) {
            log.warn("手机号绑定失败: userId={}, reason={}", userId, e.getMessage());
            return Result.badRequest(e.getMessage());
        }
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
        // 同步更新模块维度分
        radarService.updateModuleScore(request.getUserId(),
                request.getModuleType() != null ? request.getModuleType() : 1,
                Boolean.TRUE.equals(request.getIsCorrect()));
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
        // 同步更新模块维度分
        radarService.updateModuleScore(request.getUserId(),
                request.getModuleType() != null ? request.getModuleType() : 1,
                Boolean.TRUE.equals(request.getIsCorrect()));
        Map<String, Object> data = radarService.getRadarData(request.getUserId());
        log.info("定级评估: userId={}, difficulty={}, correct={}, newElo={}",
                request.getUserId(), request.getDifficultyLevel(), request.getIsCorrect(), newElo);
        return Result.success(data);
    }
}
