package cc.lingnow.admin.aspect;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.monitor.entity.SysOperLog;
import cc.lingnow.biz.monitor.service.SysOperLogService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.util.AddressUtils;
import cc.lingnow.common.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * 操作日志记录处理
 *
 * @author LingNow Team
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperLogService operLogService;
    private final SysUserService userService;

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     *
     * @param joinPoint 切点
     * @param e         异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            // 获取当前请求对象
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // *========数据库日志=========*//
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(1); // 1-成功
            // 请求的地址
            String ip = IpUtils.getIpAddr(request);
            operLog.setOperIp(ip);
            operLog.setOperUrl(request != null ? request.getRequestURI() : "");
            operLog.setOperLocation(AddressUtils.getRealAddressByIP(ip));

            if (e != null) {
                operLog.setStatus(0); // 0-失败
                operLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 2000));
            }

            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setRequestMethod(request != null ? request.getMethod() : "");

            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);

            // 设置操作时间
            operLog.setOperTime(LocalDateTime.now());
            // 简单处理消耗时间，如果需要精确需要Around或者ThreadLocal
            operLog.setCostTime(0L);

            // 设置操作人员
            String username = "未知";
            if (StpAdminUtil.isLogin()) {
                try {
                    // 1. 尝试从Session获取
                    Object userObj = StpAdminUtil.stpLogic.getSession().get("loginUser");
                    if (userObj instanceof SysUser) {
                        SysUser user = (SysUser) userObj;
                        if (userService.isInternalAccount(user)) {
                            return;
                        }
                        username = user.getUsername();
                    } else {
                        // 2. 如果Session没有，查询数据库
                        Long userId = StpAdminUtil.getLoginIdAsLong();
                        SysUser user = userService.getById(userId);
                        if (user != null) {
                            if (userService.isInternalAccount(user)) {
                                return;
                            }
                            username = user.getUsername();
                        } else {
                            username = "Admin-" + userId;
                        }
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
            operLog.setOperName(username);

            // 保存数据库
            operLogService.save(operLog);
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("==前置通知异常==");
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     *
     * @param log     日志
     * @param operLog 操作日志
     * @throws Exception
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, SysOperLog operLog, Object jsonResult) throws Exception {
        // 设置action动作
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题
        operLog.setTitle(log.title());
        // 设置操作人类别
        operLog.setOperatorType(log.operatorType().ordinal());

        // 是否需要保存request，参数和值
        if (log.isSaveRequestData()) {
            setRequestValue(joinPoint, operLog);
        }

        // 是否需要保存response，参数和值
        if (log.isSaveResponseData() && ObjectUtil.isNotNull(jsonResult)) {
            operLog.setJsonResult(StrUtil.sub(JSONUtil.toJsonStr(jsonResult), 0, 2000));
        }
    }

    /**
     * 获取请求的参数，放到log中
     *
     * @param operLog 操作日志
     * @throws Exception 异常
     */
    private void setRequestValue(JoinPoint joinPoint, SysOperLog operLog) throws Exception {
        String requestMethod = operLog.getRequestMethod();
        if ("PUT".equals(requestMethod) || "POST".equals(requestMethod)) {
            String params = argsArrayToString(joinPoint.getArgs());
            operLog.setOperParam(StrUtil.sub(params, 0, 2000));
        } else {
            String params = argsArrayToString(joinPoint.getArgs());
            operLog.setOperParam(StrUtil.sub(params, 0, 2000));
        }
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray) {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null && paramsArray.length > 0) {
            for (Object o : paramsArray) {
                if (ObjectUtil.isNotNull(o) && !isFilterObject(o)) {
                    try {
                        String str = JSONUtil.toJsonStr(o);
                        if (StrUtil.isNotEmpty(str) && !"{}".equals(str)) {
                            params.append(str).append(" ");
                        } else if (isSimpleType(o)) {
                            params.append(o.toString()).append(" ");
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        return params.toString().trim();
    }

    private boolean isSimpleType(Object o) {
        return o instanceof String || o instanceof Integer || o instanceof Long || o instanceof Boolean || o instanceof Double || o instanceof Float;
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection) o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map map = (Map) o;
            for (Object value : map.entrySet()) {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse || o instanceof BindingResult;
    }
}
