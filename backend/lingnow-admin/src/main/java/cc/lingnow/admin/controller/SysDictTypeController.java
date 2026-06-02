package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.SysDictManager;
import cc.lingnow.admin.model.bo.DictTypeQueryBO;
import cc.lingnow.admin.model.bo.DictTypeSaveBO;
import cc.lingnow.admin.model.vo.DictTypeVO;
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
 * 字典类型控制器
 *
 * @author LingNow Team
 */
@Tag(name = "字典类型管理", description = "字典类型管理接口")
@RestController
@RequestMapping("/system/dict/type")
@RequiredArgsConstructor
@Validated
public class SysDictTypeController {

    private final SysDictManager dictManager;

    @Operation(summary = "查询字典类型列表")
    @GetMapping("/list")
    public Result<PageResult<DictTypeVO>> list(DictTypeQueryBO query) {
        return Result.success(dictManager.listDictTypes(query));
    }

    @Operation(summary = "获取字典类型选项")
    @GetMapping("/optionselect")
    public Result<List<DictTypeVO>> optionselect() {
        return Result.success(dictManager.optionSelect());
    }

    @Operation(summary = "获取字典类型详细信息")
    @GetMapping("/{dictId}")
    public Result<DictTypeVO> getInfo(@PathVariable Long dictId) {
        return Result.success(dictManager.getDictType(dictId));
    }

    @Operation(summary = "新增字典类型")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DictTypeSaveBO bo) {
        dictManager.addDictType(bo);
        return Result.success();
    }

    @Operation(summary = "修改字典类型")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody DictTypeSaveBO bo) {
        dictManager.updateDictType(bo);
        return Result.success();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/{dictIds}")
    public Result<Void> remove(@PathVariable List<Long> dictIds) {
        dictManager.removeDictType(dictIds);
        return Result.success();
    }

    @Operation(summary = "刷新字典缓存")
    @DeleteMapping("/refreshCache")
    public Result<Void> refreshCache() {
        dictManager.refreshDictCache();
        return Result.success();
    }
}
