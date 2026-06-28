package com.campuslink.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，v2 新增（底层防刷能力）。
 *
 * <p>标注在 Controller 方法上后，由 {@link com.campuslink.aspect.RateLimitAspect} 拦截，
 * 在 {@link #period()} 秒的时间窗口内，同一调用方（登录用户优先按 userId，否则按 IP）
 * 对该方法的访问次数超过 {@link #count()} 时抛出
 * {@link com.campuslink.common.ResultCode#TOO_MANY_REQUESTS}。
 *
 * <p>采用进程内固定窗口计数器实现，无需引入 Redis，适配课设单机部署；
 * 后续可平滑替换为 Redis + Lua 的分布式限流。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口（秒），默认 60 秒。
     */
    int period() default 60;

    /**
     * 窗口内允许的最大请求次数，默认 20 次。
     */
    int count() default 20;

    /**
     * 限流维度：true 按登录用户/IP 隔离，false 全局共享一个计数器。
     */
    boolean perUser() default true;

    /**
     * 触发限流时的提示文案，留空使用默认文案。
     */
    String message() default "";
}
