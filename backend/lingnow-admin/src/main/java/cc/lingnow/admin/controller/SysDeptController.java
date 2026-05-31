package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.SysDeptManager;
import cc.lingnow.admin.model.bo.DeptQueryBO;
import cc.lingnow.admin.model.bo.DeptSaveBO;
import cc.lingnow.admin.model.vo.DeptVO;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 *
 * @author LingNow Team
 */
@Tag(name = "部门管理", description = "部门管理接口")
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
@Validated
public class SysDeptController {

    private final SysDeptManager deptManager;

    @Operation(summary = "获取部门列表")
    @GetMapping("/list")
    public Result<List<DeptVO>> list(DeptQueryBO query) {
        return Result.success(deptManager.listDepts(query));
    }

    @Operation(summary = "获取部门详细信息")
    @GetMapping("/{deptId}")
    public Result<DeptVO> getInfo(@PathVariable Long deptId) {
        return Result.success(deptManager.getDept(deptId));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DeptSaveBO bo) {
        deptManager.addDept(bo);
        return Result.success();
    }

    @Operation(summary = "修改部门")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody DeptSaveBO bo) {
        deptManager.updateDept(bo);
        return Result.success();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{deptId}")
    public Result<Void> remove(@PathVariable Long deptId) {
        deptManager.removeDept(deptId);
        return Result.success();
    }
}
