package cc.lingnow.core.aspect;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Web 日志切面
 * 记录所有 Controller 层的请求和响应
 *
 * @author LingNow Team
 */
@Slf4j
@Aspect
@Component
public class WebLogAspect {

    /**
     * 定义切点：拦截所有 Controller
     */
    @Pointcut("execution(* cc.lingnow..controller..*.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知：记录请求信息
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String method = request.getMethod();
            String url = request.getRequestURI();

            // 获取方法参数
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] paramValues = joinPoint.getArgs();

            log.info("========== 请求开始 ==========");
            log.info("URL: {} {}", method, url);

            // 打印参数（简化版，不打印太多细节）
            if (ArrayUtil.isNotEmpty(paramValues)) {
                for (int i = 0; i < paramValues.length; i++) {
                    Object value = paramValues[i];
                    // 过滤 HttpServletRequest/Response 等不需要打印的对象
                    if (value != null && !value.getClass().getName().startsWith("javax.servlet")
                            && !value.getClass().getName().startsWith("jakarta.servlet")
                            && !value.getClass().getName().startsWith("org.springframework")) {
                        log.info("参数[{}]: {}", paramNames != null && i < paramNames.length ? paramNames[i] : i,
                                JSONUtil.toJsonStr(value));
                    }
                }
            }
        }

        try {
            // 执行方法
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("========== 请求结束 | 耗时: {}ms ==========", executionTime);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("========== 请求异常 | 耗时: {}ms ==========", executionTime, e);
            throw e;
        }
    }

}
