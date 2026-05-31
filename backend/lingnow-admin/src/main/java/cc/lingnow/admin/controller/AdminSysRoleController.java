package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.AdminSysRoleManager;
import cc.lingnow.admin.model.bo.SysRoleBO;
import cc.lingnow.biz.role.entity.SysRole;
import cc.lingnow.biz.role.entity.SysUserRole;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class AdminSysRoleController {

    private final AdminSysRoleManager roleManager;

    @Operation(summary = "角色列表")
    @GetMapping("/list")
    @SaCheckPermission("sys:role:list")
    public Result<Page<SysRole>> list(Page<SysRole> page, SysRoleBO role) {
        return Result.success(roleManager.listRoles(page, role));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    @SaCheckPermission("sys:role:query")
    public Result<SysRole> detail(@PathVariable Long id) {
        return Result.success(roleManager.getRole(id));
    }

    @Operation(summary = "新增角色")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    @SaCheckPermission("sys:role:add")
    public Result<Boolean> add(@RequestBody @Valid SysRoleBO role) {
        return Result.success(roleManager.addRole(role));
    }

    @Operation(summary = "修改角色")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    @SaCheckPermission("sys:role:edit")
    public Result<Boolean> edit(@RequestBody @Valid SysRoleBO role) {
        return Result.success(roleManager.updateRole(role));
    }

    @Operation(summary = "数据权限")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/dataScope")
    @SaCheckPermission("sys:role:edit")
    public Result<Boolean> dataScope(@RequestBody @Valid SysRoleBO role) {
        return Result.success(roleManager.updateDataScope(role));
    }

    @Operation(summary = "删除角色")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:role:remove")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(roleManager.deleteRole(id));
    }

    @Operation(summary = "获取所有激活角色")
    @GetMapping("/active")
    public Result<List<SysRole>> activeList() {
        return Result.success(roleManager.listActiveRoles());
    }

    @Operation(summary = "分配角色给用户")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PostMapping("/assign")
    @SaCheckPermission("sys:user:edit")
    public Result<Void> assignRoles(@RequestParam Long userId, @RequestBody Long[] roleIds) {
        roleManager.assignRoles(userId, roleIds);
        return Result.success();
    }

    @Operation(summary = "查询已分配用户角色列表")
    @GetMapping("/authUser/allocatedList")
    @SaCheckPermission("sys:role:list")
    public Result<Page<SysUser>> allocatedList(Page<SysUser> page, SysUser user, Long roleId) {
        return Result.success(roleManager.selectAllocatedList(page, user, roleId));
    }

    @Operation(summary = "查询未分配用户角色列表")
    @GetMapping("/authUser/unallocatedList")
    @SaCheckPermission("sys:role:list")
    public Result<Page<SysUser>> unallocatedList(Page<SysUser> page, SysUser user, Long roleId) {
        return Result.success(roleManager.selectUnallocatedList(page, user, roleId));
    }

    @Operation(summary = "取消授权用户")
    @PutMapping("/authUser/cancel")
    @SaCheckPermission("sys:role:edit")
    public Result<Void> cancelAuthUser(@RequestBody SysUserRole userRole) {
        roleManager.deleteAuthUser(userRole);
        return Result.success();
    }

    @Operation(summary = "批量取消授权用户")
    @PutMapping("/authUser/cancelAll")
    @SaCheckPermission("sys:role:edit")
    public Result<Void> cancelAuthUserAll(Long roleId, String userIds) {
        roleManager.deleteAuthUsers(roleId, userIds);
        return Result.success();
    }

    @Operation(summary = "批量选择用户授权")
    @PutMapping("/authUser/selectAll")
    @SaCheckPermission("sys:role:edit")
    public Result<Void> selectAuthUserAll(Long roleId, String userIds) {
        roleManager.insertAuthUsers(roleId, userIds);
        return Result.success();
    }
}
