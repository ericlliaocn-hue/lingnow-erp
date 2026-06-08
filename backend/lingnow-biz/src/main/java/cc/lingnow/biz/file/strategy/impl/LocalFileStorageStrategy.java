package cc.lingnow.biz.file.strategy.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import cc.lingnow.biz.file.entity.SysFileConfig;
import cc.lingnow.biz.file.mapper.SysFileConfigMapper;
import cc.lingnow.biz.file.strategy.FileStorageStrategy;
import cc.lingnow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 本地文件存储策略
 *
 * @author LingNow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalFileStorageStrategy implements FileStorageStrategy {

    private static final String DEFAULT_BASE_PATH = "/data/lingnow/files/";
    private static final String DEFAULT_DOMAIN = "http://localhost:8090/files/";

    private final SysFileConfigMapper fileConfigMapper;

    @Override
    public String upload(MultipartFile file, String path) {
        try {
            return upload(file.getInputStream(), path, file.getOriginalFilename());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public String upload(InputStream inputStream, String path, String fileName) {
        JSONObject configJson = loadConfig();
        String basePath = normalizePath(configJson.getString("basePath"), DEFAULT_BASE_PATH);
        String domain = normalizePath(configJson.getString("domain"), DEFAULT_DOMAIN);

        // 构建完整路径
        // path如果是相对路径，如 20231010/uuid.jpg
        String fullPath = basePath + path;

        try {
            File dest = new File(fullPath);
            FileUtil.mkParentDirs(dest);
            FileUtil.writeFromStream(inputStream, dest);

            // 返回URL
            return domain + path;
        } catch (Exception e) {
            log.error("文件写入失败", e);
            throw new BusinessException("文件写入失败");
        }
    }

    @Override
    public boolean delete(String path) {
        JSONObject configJson = loadConfig();
        String basePath = normalizePath(configJson.getString("basePath"), DEFAULT_BASE_PATH);

        // path可能是完整URL，也可能是相对路径
        // 假设这里传入的是相对路径
        // 如果是URL，需要解析出相对路径
        String relativePath = path;
        if (path.startsWith("http")) {
            String domain = normalizePath(configJson.getString("domain"), DEFAULT_DOMAIN);
            if (path.startsWith(domain)) {
                relativePath = path.substring(domain.length());
            }
        }

        return FileUtil.del(basePath + relativePath);
    }

    @Override
    public String getPlatform() {
        return "LOCAL";
    }

    private JSONObject loadConfig() {
        SysFileConfig config = fileConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysFileConfig>()
                        .eq(SysFileConfig::getPlatform, "LOCAL")
                        .eq(SysFileConfig::getDelFlag, 0)
                        .last("LIMIT 1")
        );
        if (config == null || config.getConfigJson() == null || config.getConfigJson().isBlank()) {
            JSONObject defaultConfig = new JSONObject();
            defaultConfig.put("basePath", DEFAULT_BASE_PATH);
            defaultConfig.put("domain", DEFAULT_DOMAIN);
            return defaultConfig;
        }
        try {
            return JSON.parseObject(config.getConfigJson());
        } catch (Exception e) {
            log.warn("本地存储配置解析失败，使用默认配置", e);
            JSONObject defaultConfig = new JSONObject();
            defaultConfig.put("basePath", DEFAULT_BASE_PATH);
            defaultConfig.put("domain", DEFAULT_DOMAIN);
            return defaultConfig;
        }
    }

    private String normalizePath(String value, String fallback) {
        String result = (value == null || value.isBlank()) ? fallback : value;
        return result.endsWith("/") ? result : result + "/";
    }
}
