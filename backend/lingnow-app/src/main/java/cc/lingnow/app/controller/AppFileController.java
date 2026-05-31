package cc.lingnow.app.controller;

import cc.lingnow.biz.file.service.SysFileService;
import cc.lingnow.common.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 *
 * @author LingNow Team
 */
@Slf4j
@RestController
@RequestMapping("/app/file")
@RequiredArgsConstructor
@Tag(name = "文件上传")
public class AppFileController {

    private final SysFileService sysFileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    @SaCheckLogin
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        try {
            String url = sysFileService.upload(file);
            return Result.success(url);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/chunk")
    @Operation(summary = "分片上传")
    @SaCheckLogin
    public Result<Boolean> uploadChunk(
            @RequestPart("chunk") MultipartFile chunk,
            @org.springframework.web.bind.annotation.RequestParam("chunkNumber") Integer chunkNumber,
            @org.springframework.web.bind.annotation.RequestParam("totalChunks") Integer totalChunks,
            @org.springframework.web.bind.annotation.RequestParam("identifier") String identifier,
            @org.springframework.web.bind.annotation.RequestParam("filename") String filename) {
        return Result.success(sysFileService.uploadChunk(chunk, chunkNumber, totalChunks, identifier, filename));
    }

    @PostMapping("/upload/merge")
    @Operation(summary = "合并分片")
    @SaCheckLogin
    public Result<String> mergeChunks(
            @org.springframework.web.bind.annotation.RequestParam("identifier") String identifier,
            @org.springframework.web.bind.annotation.RequestParam("filename") String filename) {
        return Result.success(sysFileService.mergeChunks(identifier, filename));
    }
}
