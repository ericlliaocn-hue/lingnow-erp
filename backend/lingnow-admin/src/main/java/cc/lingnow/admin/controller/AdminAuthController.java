package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.AdminSysUserManager;
import cc.lingnow.admin.model.bo.AdminLoginBO;
import cc.lingnow.admin.model.vo.AdminLoginVO;
import cc.lingnow.admin.model.vo.LoginStatusVO;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.monitor.entity.SysLoginLog;
import cc.lingnow.biz.monitor.service.SysLoginLogService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.util.AddressUtils;
import cc.lingnow.common.util.IpUtils;
import cc.lingnow.common.vo.Result;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 管理端认证控制器
 *
 * @author LingNow Team
 */
@Slf4j
@Tag(name = "管理端认证", description = "管理员登录、登出")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AdminAuthController {

    private final AdminSysUserManager adminSysUserManager;
    private final SysLoginLogService loginLogService;
    private final SysUserService userService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginBO loginBO) {
        try {
            AdminLoginVO vo = adminSysUserManager.login(loginBO);
            recordLoginLog(loginBO.getUsername(), 1, "登录成功");
            return Result.success(vo);
        } catch (Exception e) {
            recordLoginLog(loginBO.getUsername(), 0, e.getMessage());
            throw e;
        }
    }

    private void recordLoginLog(String username, Integer status, String msg) {
        try {
            if (isInternalLoginUsername(username)) {
                return;
            }
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserName(username);
            String ip = IpUtils.getIpAddr(request);
            loginLog.setIpaddr(ip);
            loginLog.setLoginLocation(AddressUtils.getRealAddressByIP(ip));

            String userAgentStr = request != null ? request.getHeader("User-Agent") : "";
            UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
            loginLog.setBrowser(userAgent != null ? userAgent.getBrowser().getName() : "");
            loginLog.setOs(userAgent != null ? userAgent.getOs().getName() : "");

            loginLog.setStatus(status);
            loginLog.setMsg(msg);
            loginLog.setLoginTime(LocalDateTime.now());

            loginLogService.save(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志异常", e);
        }
    }

    private boolean isInternalLoginUsername(String username) {
        SysUser user = userService.getByUsername(username);
        return userService.isInternalAccount(user);
    }

    @Operation(summary = "管理员登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpAdminUtil.logout();
        return Result.success();
    }

    @Operation(summary = "获取登录状态")
    @GetMapping("/status")
    public Result<LoginStatusVO> status() {
        boolean isLogin = StpAdminUtil.isLogin();
        LoginStatusVO.LoginStatusVOBuilder builder = LoginStatusVO.builder()
                .isLogin(isLogin);

        if (isLogin) {
            builder.adminId(StpAdminUtil.getLoginIdAsLong())
                    .tokenValue(StpAdminUtil.getTokenValue());
        }
        return Result.success(builder.build());
    }

}
