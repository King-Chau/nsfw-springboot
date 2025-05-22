package com.nsfw.image;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // 每分钟最多请求次数
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    // 保存每个 IP 的请求时间戳列表
    private static final Map<String, List<Long>> ipRequestMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIP(request);
        long now = Instant.now().getEpochSecond();

        List<Long> timestamps = ipRequestMap.computeIfAbsent(ip, k -> new ArrayList<>());

        synchronized (timestamps) {
            // 移除超过 60 秒的请求
            timestamps.removeIf(t -> now - t > 60);

            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\": false, \"error\": \"请求太频繁，请稍后再试\"}");
                return false;
            }

            timestamps.add(now);
        }

        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0]; // 取第一个
        }
        return ip;
    }
}
