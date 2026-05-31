package cc.lingnow.app.controller;

import cc.lingnow.app.manager.AppUserManager;
import cc.lingnow.app.model.bo.AppPasswordChangeBO;
import cc.lingnow.app.model.bo.AppPhoneChangeBO;
import cc.lingnow.app.model.bo.AppUserProfileUpdateBO;
import cc.lingnow.app.model.vo.AppUserVO;
import cc.lingnow.common.vo.Result;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "App用户接口")
@RestController
@RequestMapping("/app/user")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserManager appUserManager;

    @Operation(summary = "获取个人资料")
    @GetMapping("/profile")
    public Result<AppUserVO> getProfile() {
        return Result.success(appUserManager.getProfile(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody AppUserProfileUpdateBO updateBO) {
        appUserManager.updateProfile(StpUtil.getLoginIdAsLong(), updateBO);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody @Valid AppPasswordChangeBO changeBO) {
        appUserManager.changePassword(StpUtil.getLoginIdAsLong(), changeBO);
        return Result.success();
    }

    @Operation(summary = "修改手机号")
    @PostMapping("/phone")
    public Result<Void> changePhone(@RequestBody @Valid AppPhoneChangeBO changeBO) {
        appUserManager.changePhone(StpUtil.getLoginIdAsLong(), changeBO);
        return Result.success();
    }
}
