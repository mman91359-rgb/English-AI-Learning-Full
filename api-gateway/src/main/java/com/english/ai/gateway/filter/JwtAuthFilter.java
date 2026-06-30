package com.english.ai.gateway.filter;

import com.english.ai.common.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 全局认证过滤器
 *
 * 白名单路径放行，其余路径校验 Token。
 * 校验通过后将 userId 和 username 通过 X-User-Id / X-Username Header 透传给下游服务。
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    /** 无需认证的路径 */
    private static final List<String> WHITELIST = List.of(
            "/user/register",
            "/user/login",
            // 头像静态资源（浏览器 <img> 加载无法携带 JWT，单独放行）
            "/user/avatar/"
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 提取 Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少认证Token");
        }
        String token = authHeader.substring(7);

        try {
            // 校验Token
            Claims claims = JwtUtil.parse(token, jwtSecret);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);

            // 透传用户信息到下游
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username)
                    .build();

            log.debug("JWT校验通过: userId={}, path={}", userId, path);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.warn("JWT校验失败: path={}, reason={}", path, e.getMessage());
            return unauthorized(exchange, "Token无效或已过期");
        }
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
