package cc.lingnow.admin.controller;

import cc.lingnow.biz.gen.bo.GenTableQueryBO;
import cc.lingnow.biz.gen.entity.GenTable;
import cc.lingnow.biz.gen.service.GenService;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 代码生成 操作处理
 *
 * @author lingnow
 */
@Tag(name = "代码生成", description = "代码生成相关接口")
@RestController
@RequestMapping("/tool/gen")
@RequiredArgsConstructor
public class GenController {

    private final GenService genService;

    @Operation(summary = "查询代码生成列表")
    @GetMapping("/list")
    public Result<List<GenTable>> genList(GenTable genTable) {
        List<GenTable> list = genService.selectGenTableList(genTable);
        return Result.success(list);
    }

    @Operation(summary = "查询数据库列表")
    @GetMapping("/db/list")
    public Result<List<GenTable>> dataList(GenTableQueryBO genTableQueryBO) {
        List<GenTable> list = genService.selectDbTableList(genTableQueryBO);
        return Result.success(list);
    }

    @Operation(summary = "导入表结构（保存）")
    @PostMapping("/importTable")
    public Result<Void> importTableSave(String tables) {
        String[] tableNames = tables.split(",");
        genService.importGenTable(tableNames);
        return Result.success();
    }

    @Operation(summary = "同步数据库")
    @PostMapping("/synchDb/{tableName}")
    public Result<Void> synchDb(@PathVariable("tableName") String tableName) {
        genService.synchDb(tableName);
        return Result.success();
    }

    @Operation(summary = "修改代码生成业务")
    @PutMapping
    public Result<Void> edit(@RequestBody GenTable genTable) {
        genService.validateEdit(genTable);
        genService.updateGenTable(genTable);
        return Result.success();
    }

    @Operation(summary = "获取代码生成业务详情")
    @GetMapping(value = "/{tableId}")
    public Result<GenTable> getInfo(@PathVariable Long tableId) {
        GenTable table = genService.selectGenTableById(tableId);
        return Result.success(table);
    }

    @Operation(summary = "删除代码生成")
    @DeleteMapping("/{tableIds}")
    public Result<Void> remove(@PathVariable Long[] tableIds) {
        genService.deleteGenTableByIds(tableIds);
        return Result.success();
    }

    @Operation(summary = "预览代码")
    @GetMapping("/preview/{tableName}")
    public Result<Map<String, String>> preview(@PathVariable("tableName") String tableName) {
        Map<String, String> dataMap = genService.previewCode(tableName);
        return Result.success(dataMap);
    }

    @Operation(summary = "生成代码（下载方式）")
    @GetMapping("/download/{tableName}")
    public void download(HttpServletResponse response, @PathVariable("tableName") String tableName) throws IOException {
        byte[] data = genService.downloadCode(tableName);
        genCode(response, data);
    }

    @Operation(summary = "批量生成代码")
    @GetMapping("/batchGenCode")
    public void batchGenCode(HttpServletResponse response, String tables) throws IOException {
        String[] tableNames = tables.split(",");
        byte[] data = genService.downloadCode(tableNames);
        genCode(response, data);
    }

    /**
     * 生成zip文件
     */
    private void genCode(HttpServletResponse response, byte[] data) throws IOException {
        response.reset();
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Content-Disposition", "attachment; filename=\"lingnow.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(data, response.getOutputStream());
    }
}
