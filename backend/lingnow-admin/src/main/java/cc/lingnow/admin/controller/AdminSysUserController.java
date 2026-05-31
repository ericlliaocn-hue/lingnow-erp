package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.AdminSysUserManager;
import cc.lingnow.admin.model.bo.AdminUserUpdateBO;
import cc.lingnow.admin.model.bo.UserQueryBO;
import cc.lingnow.admin.model.vo.UserDetailVO;
import cc.lingnow.admin.model.vo.UserListVO;
import cc.lingnow.admin.model.vo.UserStatsVO;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器 (管理C端用户)
 *
 * @author LingNow Team
 */
@Tag(name = "用户管理", description = "管理C端用户信息")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class AdminSysUserController {

    private final AdminSysUserManager adminSysUserManager;

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/list")
    public Result<PageResult<UserListVO>> listUsers(@Valid UserQueryBO query) {
        PageResult<UserListVO> result = adminSysUserManager.listUsers(query);
        return Result.success(result);
    }

    @Operation(summary = "获取用户统计信息")
    @GetMapping("/stats")
    public Result<UserStatsVO> stats() {
        UserStatsVO stats = adminSysUserManager.getUserStats();
        return Result.success(stats);
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    public Result<UserDetailVO> getUser(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        UserDetailVO user = adminSysUserManager.getUserDetail(id);
        return Result.success(user);
    }

    @Operation(summary = "更新用户信息")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> updateUser(
            @PathVariable @NotNull(message = "用户ID不能为空") Long id,
            @Valid @RequestBody AdminUserUpdateBO updateBO) {
        adminSysUserManager.updateUser(id, updateBO);
        return Result.success();
    }

    @Operation(summary = "禁用用户")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/disable")
    public Result<Void> disableUser(
            @PathVariable @NotNull(message = "用户ID不能为空") Long id,
            @RequestParam(required = false) String reason) {
        adminSysUserManager.disableUser(id, reason);
        return Result.success();
    }

    @Operation(summary = "启用用户")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/enable")
    public Result<Void> enableUser(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        adminSysUserManager.enableUser(id);
        return Result.success();
    }

    @Operation(summary = "更新用户状态")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable @NotNull(message = "用户ID不能为空") Long id,
            @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        adminSysUserManager.updateUserStatus(id, status);
        return Result.success();
    }

}
