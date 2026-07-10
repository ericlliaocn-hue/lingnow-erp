package cc.lingnow.admin.controller;

import cc.lingnow.biz.file.entity.SysFileConfig;
import cc.lingnow.biz.file.mapper.SysFileConfigMapper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Public local file reader for /files/** URLs.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LocalFileController {

    private static final String DEFAULT_FILE_BASE_PATH = "/data/lingnow/files/";
    private static final String FILE_PREFIX = "/files/";

    private final SysFileConfigMapper fileConfigMapper;

    @GetMapping("/files/**")
    public void readFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String relativePath = extractRelativePath(request);
        if (relativePath.isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path basePath = Path.of(loadBasePath()).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(relativePath).normalize();
        if (!filePath.startsWith(basePath) || !Files.isRegularFile(filePath)) {
            log.warn("文件不存在或路径非法: {}", relativePath);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(filePath);
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=604800");
        response.setContentLengthLong(Files.size(filePath));
        Files.copy(filePath, response.getOutputStream());
    }

    private String extractRelativePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (!uri.startsWith(FILE_PREFIX)) {
            return "";
        }
        return uri.substring(FILE_PREFIX.length());
    }

    private String loadBasePath() {
        SysFileConfig config = fileConfigMapper.selectOne(
                new LambdaQueryWrapper<SysFileConfig>()
                        .eq(SysFileConfig::getPlatform, "LOCAL")
                        .eq(SysFileConfig::getDelFlag, 0)
                        .last("LIMIT 1")
        );
        if (config == null || config.getConfigJson() == null || config.getConfigJson().isBlank()) {
            return DEFAULT_FILE_BASE_PATH;
        }
        try {
            JSONObject configJson = JSON.parseObject(config.getConfigJson());
            String basePath = configJson.getString("basePath");
            return basePath == null || basePath.isBlank() ? DEFAULT_FILE_BASE_PATH : basePath;
        } catch (Exception e) {
            log.warn("本地文件配置解析失败，使用默认目录", e);
            return DEFAULT_FILE_BASE_PATH;
        }
    }
}
