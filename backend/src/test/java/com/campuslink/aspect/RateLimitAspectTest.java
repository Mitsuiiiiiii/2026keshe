package com.campuslink.aspect;

import com.campuslink.annotation.RateLimit;
import com.campuslink.common.BusinessException;
import com.campuslink.common.ResultCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RateLimitAspect} 限流切面单元测试。
 *
 * <p>用真实带注解的方法提供 {@link RateLimit} 实例，配合 Mockito 模拟连接点，
 * 验证「窗口内超过阈值抛 429」。未配置 SecurityContext / 请求上下文时，
 * 切面按匿名维度计数，正好满足测试无容器环境。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
class RateLimitAspectTest {

    private RateLimitAspect aspect;
    private RateLimit limitTwoPerMinute;

    /** 仅用于承载 @RateLimit 注解，供反射读取。 */
    @RateLimit(period = 60, count = 2, perUser = true)
    void guarded() {
    }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        aspect = new RateLimitAspect();
        Method method = RateLimitAspectTest.class.getDeclaredMethod("guarded");
        limitTwoPerMinute = method.getAnnotation(RateLimit.class);
    }

    private ProceedingJoinPoint mockJoinPoint() throws Throwable {
        Method method = RateLimitAspectTest.class.getDeclaredMethod("guarded");
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenReturn("ok");
        return pjp;
    }

    @Test
    @DisplayName("窗口内未超阈值正常放行")
    void allowWithinLimit() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint();
        assertEquals("ok", aspect.around(pjp, limitTwoPerMinute));
        assertEquals("ok", aspect.around(pjp, limitTwoPerMinute));
        verify(pjp, times(2)).proceed();
    }

    @Test
    @DisplayName("超过阈值抛出 429 限流异常")
    void blockOverLimit() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint();
        aspect.around(pjp, limitTwoPerMinute);
        aspect.around(pjp, limitTwoPerMinute);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.around(pjp, limitTwoPerMinute));
        assertEquals(ResultCode.TOO_MANY_REQUESTS.getCode(), ex.getCode());
        // 第 3 次被拦截，proceed 仍只调用 2 次
        verify(pjp, times(2)).proceed();
    }

    @Test
    @DisplayName("不同切面实例计数器相互独立")
    void countersIsolatedPerAspectInstance() throws Throwable {
        ProceedingJoinPoint pjp = mockJoinPoint();
        aspect.around(pjp, limitTwoPerMinute);
        aspect.around(pjp, limitTwoPerMinute);

        RateLimitAspect freshAspect = new RateLimitAspect();
        ProceedingJoinPoint pjp2 = mockJoinPoint();
        assertDoesNotThrow(() -> freshAspect.around(pjp2, limitTwoPerMinute));
    }
}
