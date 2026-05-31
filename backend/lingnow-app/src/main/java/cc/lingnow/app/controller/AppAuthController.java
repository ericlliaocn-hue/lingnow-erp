package cc.lingnow.app.controller;

import cc.lingnow.app.manager.AppUserManager;
import cc.lingnow.app.model.bo.AppLoginBO;
import cc.lingnow.app.model.bo.AppRegisterBO;
import cc.lingnow.app.model.vo.AppUserVO;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.Result;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "App认证接口")
@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final AppUserManager appUserManager;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<AppUserVO> login(@RequestBody @Valid AppLoginBO loginBO) {
        return Result.success(appUserManager.login(loginBO));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<AppUserVO> register(@RequestBody @Valid AppRegisterBO registerBO) {
        return Result.success(appUserManager.register(registerBO));
    }

    @Operation(summary = "忘记密码")
    @PostMapping("/forget-password")
    public Result<Void> forgetPassword(@RequestBody @Valid AppRegisterBO bo) {
        appUserManager.forgetPassword(bo.getPhone(), bo.getCode(), bo.getPassword());
        return Result.success();
    }

    @Operation(summary = "发送验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestParam String phone) {
        throw new BusinessException(ErrorCode.MESSAGE_SEND_FAILED, "短信服务未配置");
    }

    @Operation(summary = "校验验证码")
    @PostMapping("/validate-code")
    public Result<Void> validateCode(@RequestParam String phone, @RequestParam String code) {
        appUserManager.validateCode(phone, code);
        return Result.success();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
