package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.SysDictManager;
import cc.lingnow.admin.model.bo.DictDataQueryBO;
import cc.lingnow.admin.model.bo.DictDataSaveBO;
import cc.lingnow.admin.model.vo.DictDataVO;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典数据控制器
 *
 * @author LingNow Team
 */
@Tag(name = "字典数据管理", description = "字典数据管理接口")
@RestController
@RequestMapping("/system/dict/data")
@RequiredArgsConstructor
@Validated
public class SysDictDataController {

    private final SysDictManager dictManager;

    @Operation(summary = "查询字典数据列表")
    @GetMapping("/list")
    public Result<List<DictDataVO>> list(DictDataQueryBO query) {
        return Result.success(dictManager.listDictData(query));
    }

    @Operation(summary = "获取字典数据详细信息")
    @GetMapping("/{dictCode}")
    public Result<DictDataVO> getInfo(@PathVariable Long dictCode) {
        return Result.success(dictManager.getDictData(dictCode));
    }

    @Operation(summary = "根据字典类型查询字典数据信息")
    @GetMapping("/type/{dictType}")
    public Result<List<DictDataVO>> getDicts(@PathVariable String dictType) {
        return Result.success(dictManager.getDictDataByType(dictType));
    }

    @Operation(summary = "新增字典数据")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DictDataSaveBO bo) {
        dictManager.addDictData(bo);
        return Result.success();
    }

    @Operation(summary = "修改字典数据")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody DictDataSaveBO bo) {
        dictManager.updateDictData(bo);
        return Result.success();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/{dictCodes}")
    public Result<Void> remove(@PathVariable List<Long> dictCodes) {
        dictManager.removeDictData(dictCodes);
        return Result.success();
    }
}
