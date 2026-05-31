package cc.lingnow.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.event.ErrorLogEvent;
import cc.lingnow.common.util.IpUtils;
import cc.lingnow.common.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author LingNow Team
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常: {}", e.getMessage());
        publishErrorLog(e, request);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLoginException(NotLoginException e) {
        log.error("未登录异常: {}", e.getMessage());
        return Result.error(ErrorCode.NOT_LOGIN.getCode(), "请先登录");
    }

    /**
     * Sa-Token 权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNotPermissionException(NotPermissionException e) {
        log.error("权限不足异常: {}", e.getMessage());
        return Result.error(ErrorCode.NO_AUTH.getCode(), "权限不足");
    }

    /**
     * Sa-Token 角色不足异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNotRoleException(NotRoleException e) {
        log.error("角色不足异常: {}", e.getMessage());
        return Result.error(ErrorCode.NO_AUTH.getCode(), "角色不足");
    }

    /**
     * 参数校验异常（@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数校验异常: {}", message);
        return Result.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数绑定异常: {}", message);
        return Result.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常: {}", e.getMessage());
        return Result.error(ErrorCode.PARAMS_ERROR.getCode(), e.getMessage());
    }

    /**
     * 静态资源不存在异常
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Result<?> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        // 仅记录警告日志，不打印堆栈
        log.warn("静态资源不存在: {}", e.getResourcePath());
        return Result.error(ErrorCode.NOT_FOUND.getCode(), "资源不存在: " + e.getResourcePath());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: ", e);

        // 发布错误日志事件
        publishErrorLog(e, request);

        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统异常，请联系管理员");
    }

    private void publishErrorLog(Exception e, HttpServletRequest request) {
        try {
            String requestUrl = request.getRequestURI();
            String requestMethod = request.getMethod();
            String ip = IpUtils.getIpAddr(request);
            String requestParams = JSONUtil.toJsonStr(request.getParameterMap());

            // 获取异常堆栈
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String errorStack = sw.toString();

            Long userId = null;
            String userName = null;
            try {
                if (StpUtil.isLogin()) {
                    userId = StpUtil.getLoginIdAsLong();
                    // 这里简化处理，不强求获取用户名，或者通过LoginHelper获取
                    // userName = LoginHelper.getLoginUser().getUsername(); 
                }
            } catch (Exception ex) {
                // ignore
            }

            ErrorLogEvent event = new ErrorLogEvent(this, requestUrl, requestMethod, requestParams, ip,
                    e.getMessage(), errorStack, userId, userName);
            eventPublisher.publishEvent(event);
        } catch (Exception ex) {
            log.error("发布错误日志事件失败", ex);
        }
    }
}
