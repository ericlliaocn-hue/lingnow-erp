package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.FileConfigUpdateBO;
import cc.lingnow.admin.model.bo.FileQueryBO;
import cc.lingnow.admin.model.vo.FileConfigVO;
import cc.lingnow.admin.model.vo.FileVO;
import cc.lingnow.biz.file.entity.SysFile;
import cc.lingnow.biz.file.entity.SysFileConfig;
import cc.lingnow.biz.file.service.SysFileConfigService;
import cc.lingnow.biz.file.service.SysFileService;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理控制器
 *
 * @author LingNow Team
 */
@RestController
@RequestMapping("/admin/file")
@RequiredArgsConstructor
@Tag(name = "文件管理")
@SaCheckLogin
public class AdminSysFileController {

    private final SysFileService sysFileService;
    private final SysFileConfigService sysFileConfigService;

    @GetMapping("/page")
    @Operation(summary = "文件列表")
    public Result<PageResult<FileVO>> page(FileQueryBO query) {
        Page<SysFile> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<SysFile>()
                .like(StringUtils.hasText(query.getFileName()), SysFile::getFileName, query.getFileName())
                .eq(StringUtils.hasText(query.getStorageType()), SysFile::getStorageType, query.getStorageType())
                .orderByDesc(SysFile::getCreateTime);

        sysFileService.page(page, wrapper);

        List<FileVO> list = BeanUtil.copyToList(page.getRecords(), FileVO.class);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), list));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(sysFileService.deleteFile(id));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(sysFileService.upload(file));
    }

    @PostMapping("/upload/chunk")
    @Operation(summary = "分片上传")
    public Result<Boolean> uploadChunk(
            @RequestPart("chunk") MultipartFile chunk,
            @RequestParam("chunkNumber") Integer chunkNumber,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("identifier") String identifier,
            @RequestParam("filename") String filename) {
        return Result.success(sysFileService.uploadChunk(chunk, chunkNumber, totalChunks, identifier, filename));
    }

    @PostMapping("/upload/merge")
    @Operation(summary = "合并分片")
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    public Result<String> mergeChunks(
            @RequestParam("identifier") String identifier,
            @RequestParam("filename") String filename) {
        return Result.success(sysFileService.mergeChunks(identifier, filename));
    }

    @GetMapping("/config/list")
    @Operation(summary = "获取配置列表")
    public Result<List<FileConfigVO>> configList() {
        List<SysFileConfig> list = sysFileConfigService.list();
        return Result.success(BeanUtil.copyToList(list, FileConfigVO.class));
    }

    @PostMapping("/config")
    @Operation(summary = "保存/更新配置")
    @Log(title = "文件管理", businessType = BusinessType.UPDATE)
    public Result<Boolean> saveConfig(@RequestBody FileConfigUpdateBO bo) {
        SysFileConfig config = BeanUtil.copyProperties(bo, SysFileConfig.class);
        sysFileConfigService.updateConfig(config);
        return Result.success(true);
    }
}
