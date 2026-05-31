package cc.lingnow.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cc.lingnow.common.annotation.RateLimiter;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.enums.LimitType;
import cc.lingnow.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流处理
 *
 * @author LingNow Team
 */
@Aspect
@Component
@Slf4j
public class RateLimiterAspect {

    @Autowired
    private RedissonClient redissonClient;

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable {
        int time = rateLimiter.time();
        int count = rateLimiter.count();

        String combineKey = getCombineKey(rateLimiter, point);

        // 使用 Redisson 的 RateLimiter 或者 AtomicLong 配合 expire 实现
        // 这里为了简单和精确控制 "几秒内几次"，使用 RRateLimiter 可能有点重，
        // 且 RRateLimiter 是令牌桶算法，这里更像是滑动窗口或者固定窗口计数。
        // 为了"防重复提交"（几秒内1次），用 SetNX 也可以。
        // 为了"限流"（几秒内N次），用计数器比较合适。

        // 我们使用最通用的 Redis 计数器模式 (Fixed Window)
        try {
            long number = redissonClient.getAtomicLong(combineKey).incrementAndGet();
            if (number == 1) {
                redissonClient.getAtomicLong(combineKey).expire(time, TimeUnit.SECONDS);
            }

            if (number > count) {
                // 超过限制
                throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "访问过于频繁，请稍候再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("限流异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍候再试");
        }
    }

    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        StringBuilder stringBuilder = new StringBuilder(rateLimiter.key());

        // 加上方法名，保证唯一性
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuilder.append(targetClass.getName()).append("-").append(method.getName());

        if (rateLimiter.limitType() == LimitType.IP) {
            stringBuilder.append("-").append(getIpAddress());
        } else if (rateLimiter.limitType() == LimitType.USER) {
            // 如果用户未登录，回退到 IP 限制
            if (StpUtil.isLogin()) {
                stringBuilder.append("-").append(StpUtil.getLoginId());
            } else {
                stringBuilder.append("-").append(getIpAddress());
            }
        }
        return stringBuilder.toString();
    }

    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 暂时手动获取，避免 Hutool 5.x javax/jakarta 冲突
                String ip = request.getHeader("x-forwarded-for");
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
            }
        } catch (Exception e) {
            // ignore
        }
        return "unknown";
    }
}
