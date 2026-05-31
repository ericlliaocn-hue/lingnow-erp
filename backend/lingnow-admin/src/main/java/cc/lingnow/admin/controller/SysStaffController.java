package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.SysStaffManager;
import cc.lingnow.admin.model.bo.SysStaffAuthRoleBO;
import cc.lingnow.admin.model.bo.SysStaffQueryBO;
import cc.lingnow.admin.model.bo.SysStaffResetPwdBO;
import cc.lingnow.admin.model.bo.SysStaffSaveBO;
import cc.lingnow.admin.model.vo.SysStaffVO;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工管理控制器
 *
 * @author LingNow Team
 */
@Tag(name = "员工管理", description = "员工管理接口")
@RestController
@RequestMapping("/system/staff")
@RequiredArgsConstructor
@Validated
public class SysStaffController {

    private final SysStaffManager staffManager;

    @Operation(summary = "获取员工列表")
    @GetMapping("/list")
    public Result<PageResult<SysStaffVO>> list(SysStaffQueryBO query) {
        return Result.success(staffManager.listStaff(query));
    }

    @Operation(summary = "获取员工详细信息")
    @GetMapping("/{userId}")
    public Result<SysStaffVO> getInfo(@PathVariable Long userId) {
        return Result.success(staffManager.getStaff(userId));
    }

    @Operation(summary = "新增员工")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody SysStaffSaveBO bo) {
        staffManager.addStaff(bo);
        return Result.success();
    }

    @Operation(summary = "修改员工")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody SysStaffSaveBO bo) {
        staffManager.updateStaff(bo);
        return Result.success();
    }

    @Operation(summary = "删除员工")
    @DeleteMapping("/{userIds}")
    public Result<Void> remove(@PathVariable List<Long> userIds) {
        staffManager.removeStaff(userIds);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/reset-pwd")
    public Result<Void> resetPwd(@RequestBody SysStaffResetPwdBO bo) {
        staffManager.resetPassword(bo.getUserId(), bo.getPassword());
        return Result.success();
    }

    @Operation(summary = "分配角色")
    @PutMapping("/auth-role")
    public Result<Void> authRole(@RequestBody SysStaffAuthRoleBO bo) {
        staffManager.assignRoles(bo.getUserId(), bo.getRoleIds());
        return Result.success();
    }
}
