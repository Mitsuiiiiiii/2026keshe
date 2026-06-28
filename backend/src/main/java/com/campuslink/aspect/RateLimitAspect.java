package com.campuslink.aspect;

import com.campuslink.annotation.RateLimit;
import com.campuslink.common.BusinessException;
import com.campuslink.common.ResultCode;
import com.campuslink.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口限流切面，v2 新增。
 *
 * <p>拦截带 {@link RateLimit} 注解的方法，采用进程内固定窗口计数器实现防刷。
 * 计数 key 由「方法全限定名 + 调用方标识」组成，窗口到期后整桶重置。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitAspect.class);

    /** key -> 计数窗口 */
    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = buildKey(pjp, rateLimit);
        long nowMs = System.currentTimeMillis();
        long windowMs = rateLimit.period() * 1000L;

        Window window = counters.compute(key, (k, old) -> {
            if (old == null || nowMs - old.startMs >= windowMs) {
                return new Window(nowMs);
            }
            return old;
        });

        int current = window.count.incrementAndGet();
        if (current > rateLimit.count()) {
            String msg = rateLimit.message().isEmpty()
                    ? ResultCode.TOO_MANY_REQUESTS.getMessage()
                    : rateLimit.message();
            LOGGER.warn("[RATE_LIMIT] 触发限流, key={}, count={}/{}", key, current, rateLimit.count());
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, msg);
        }
        return pjp.proceed();
    }

    /**
     * 计数 key：方法签名 + 调用方维度。
     */
    private String buildKey(ProceedingJoinPoint pjp, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String base = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        if (!rateLimit.perUser()) {
            return base;
        }
        return base + ":" + resolveCaller();
    }

    /**
     * 解析调用方：优先登录用户 id，其次客户端 IP。
     */
    private String resolveCaller() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return "u" + loginUser.getUserId();
        }
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return "ip" + forwarded.split(",")[0].trim();
            }
            return "ip" + request.getRemoteAddr();
        }
        return "anonymous";
    }

    /**
     * 固定窗口计数桶。
     */
    static class Window {
        final long startMs;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMs) {
            this.startMs = startMs;
        }
    }
}
