package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.SysConfigManager;
import cc.lingnow.admin.model.bo.ConfigQueryBO;
import cc.lingnow.admin.model.bo.ConfigSaveBO;
import cc.lingnow.admin.model.vo.ConfigVO;
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
 * 参数配置控制器
 *
 * @author LingNow Team
 */
@Tag(name = "参数配置管理", description = "参数配置管理接口")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@Validated
public class SysConfigController {

    private final SysConfigManager configManager;

    @Operation(summary = "获取参数配置列表")
    @GetMapping("/list")
    public Result<PageResult<ConfigVO>> list(ConfigQueryBO query) {
        return Result.success(configManager.listConfigs(query));
    }

    @Operation(summary = "获取参数配置详细信息")
    @GetMapping("/{configId}")
    public Result<ConfigVO> getInfo(@PathVariable Long configId) {
        return Result.success(configManager.getConfig(configId));
    }

    @Operation(summary = "新增参数配置")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody ConfigSaveBO bo) {
        configManager.addConfig(bo);
        return Result.success();
    }

    @Operation(summary = "修改参数配置")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody ConfigSaveBO bo) {
        configManager.updateConfig(bo);
        return Result.success();
    }

    @Operation(summary = "刷新参数缓存")
    @DeleteMapping("/refreshCache")
    public Result<Void> refreshCache() {
        configManager.refreshConfigCache();
        return Result.success();
    }

    @Operation(summary = "删除参数配置")
    @DeleteMapping("/{configIds}")
    public Result<Void> remove(@PathVariable List<Long> configIds) {
        configManager.removeConfig(configIds);
        return Result.success();
    }
}
