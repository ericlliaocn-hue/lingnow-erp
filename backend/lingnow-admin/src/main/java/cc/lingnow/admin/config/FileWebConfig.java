package cc.lingnow.admin.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import cc.lingnow.biz.file.entity.SysFileConfig;
import cc.lingnow.biz.file.mapper.SysFileConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文件资源映射配置
 *
 * @author LingNow Team
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileWebConfig implements WebMvcConfigurer {

    private static final String DEFAULT_FILE_BASE_PATH = "/data/lingnow/files/";
    private static final String DEFAULT_FILE_DOMAIN = "/files/";

    private final SysFileConfigMapper fileConfigMapper;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始配置静态资源映射...");
        try {
            // 查询本地存储配置
            SysFileConfig config = fileConfigMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysFileConfig>()
                            .eq(SysFileConfig::getPlatform, "LOCAL")
            );

            if (config != null) {
                JSONObject configJson = JSON.parseObject(config.getConfigJson());
                String basePath = configJson.getString("basePath");
                String domain = configJson.getString("domain");

                log.info("找到本地配置: basePath={}, domain={}", basePath, domain);

                if (basePath != null && domain != null) {
                    // 提取映射路径
                    String pathPattern = normalizeDomainPath(domain);

                    // 确保basePath以/结尾
                    if (!basePath.endsWith("/")) {
                        basePath += "/";
                    }

                    String location = java.nio.file.Paths.get(basePath).toAbsolutePath().normalize().toUri().toString();

                    log.info("映射静态资源: {} -> {}", pathPattern, location);
                    registry.addResourceHandler(pathPattern)
                            .addResourceLocations(location);
                }
            } else {
                // 默认映射
                String defaultPath = defaultBasePath();
                log.info("未找到本地存储配置，使用默认映射: /files/** -> file:{}", defaultPath);
                registry.addResourceHandler("/files/**")
                        .addResourceLocations(java.nio.file.Paths.get(defaultPath).toAbsolutePath().normalize().toUri().toString());
            }
        } catch (Exception e) {
            log.error("加载文件资源映射失败", e);
            // Fallback
                registry.addResourceHandler(DEFAULT_FILE_DOMAIN + "**")
                    .addResourceLocations(java.nio.file.Paths.get(defaultBasePath()).toAbsolutePath().normalize().toUri().toString());
        }
    }

    private String defaultBasePath() {
        String path = System.getenv().getOrDefault("LINGNOW_FILE_BASE_PATH", DEFAULT_FILE_BASE_PATH);
        return path.endsWith("/") ? path : path + "/";
    }

    private String normalizeDomainPath(String domain) {
        if (domain == null || domain.isBlank()) {
            return DEFAULT_FILE_DOMAIN + "**";
        }
        if (domain.startsWith("http")) {
            try {
                java.net.URL url = new java.net.URL(domain);
                String pathPattern = url.getPath();
                if (!pathPattern.endsWith("/")) {
                    pathPattern += "/";
                }
                return pathPattern + "**";
            } catch (Exception e) {
                log.warn("解析域名失败，使用默认 /files/**: {}", domain);
                return DEFAULT_FILE_DOMAIN + "**";
            }
        }
        String pathPattern = domain;
        if (!pathPattern.endsWith("/")) {
            pathPattern += "/";
        }
        if (!pathPattern.startsWith("/")) {
            pathPattern = "/" + pathPattern;
        }
        return pathPattern + "**";
    }
}
