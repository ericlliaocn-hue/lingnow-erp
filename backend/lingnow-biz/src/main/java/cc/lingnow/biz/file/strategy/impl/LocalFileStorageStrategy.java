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
        // 获取配置
        SysFileConfig config = fileConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysFileConfig>()
                        .eq(SysFileConfig::getPlatform, "LOCAL")
        );

        if (config == null) {
            throw new BusinessException("本地存储配置未初始化");
        }

        JSONObject configJson = JSON.parseObject(config.getConfigJson());
        String basePath = configJson.getString("basePath");
        String domain = configJson.getString("domain");

        // 确保basePath以/结尾
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        // 确保domain以/结尾
        if (!domain.endsWith("/")) {
            domain += "/";
        }

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
        // 获取配置
        SysFileConfig config = fileConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysFileConfig>()
                        .eq(SysFileConfig::getPlatform, "LOCAL")
        );
        if (config == null) {
            return false;
        }
        JSONObject configJson = JSON.parseObject(config.getConfigJson());
        String basePath = configJson.getString("basePath");
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        // path可能是完整URL，也可能是相对路径
        // 假设这里传入的是相对路径
        // 如果是URL，需要解析出相对路径
        String relativePath = path;
        if (path.startsWith("http")) {
            String domain = configJson.getString("domain");
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
}
