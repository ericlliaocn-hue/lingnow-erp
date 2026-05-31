package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.AdminSysMenuManager;
import cc.lingnow.admin.model.bo.SysMenuBO;
import cc.lingnow.admin.model.vo.SysMenuVO;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统菜单控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/menu")
public class AdminSysMenuController {

    private final AdminSysMenuManager menuManager;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    public Result<List<SysMenuVO>> getMenuTree() {
        return Result.success(menuManager.getMenuTree());
    }

    /**
     * 获取菜单树
     */
    @GetMapping("/tree/all")
    public Result<List<SysMenuVO>> getAllMenuTree() {
        return Result.success(menuManager.getAllMenuTree());
    }

    /**
     * 获取所有菜单
     */
    @GetMapping("/list")
    public Result<List<SysMenuVO>> getAllMenus() {
        return Result.success(menuManager.listMenus());
    }

    /**
     * 根据ID获取菜单
     */
    @GetMapping("/{id}")
    public Result<SysMenuVO> getById(@PathVariable Long id) {
        return Result.success(menuManager.getMenu(id));
    }

    /**
     * 新增菜单
     */
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<Boolean> add(@RequestBody @Valid SysMenuBO menu) {
        return Result.success(menuManager.addMenu(menu));
    }

    /**
     * 修改菜单
     */
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public Result<Boolean> update(@RequestBody @Valid SysMenuBO menu) {
        return Result.success(menuManager.updateMenu(menu));
    }

    /**
     * 删除菜单
     */
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(menuManager.deleteMenu(id));
    }
}
