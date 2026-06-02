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
                    String pathPattern = "/files/**";
                    try {
                        java.net.URL url = new java.net.URL(domain);
                        pathPattern = url.getPath();
                        if (!pathPattern.endsWith("/")) {
                            pathPattern += "/";
                        }
                        pathPattern += "**";
                    } catch (Exception e) {
                        log.warn("解析域名失败，使用默认 /files/**: {}", domain);
                    }

                    // 确保basePath以/结尾
                    if (!basePath.endsWith("/")) {
                        basePath += "/";
                    }

                    // 修正：macOS/Linux下 file: 协议后跟绝对路径建议使用 file://
                    // 如果 basePath 已经是 /Users/... 开头，则 "file:" + basePath 变成 "file:/Users/..."
                    // 为了保险，我们可以尝试使用 "file:" + basePath

                    String location = "file:" + basePath;

                    log.info("映射静态资源: {} -> {}", pathPattern, location);
                    registry.addResourceHandler(pathPattern)
                            .addResourceLocations(location);
                }
            } else {
                // 默认映射
                String defaultPath = defaultBasePath();
                log.info("未找到本地存储配置，使用默认映射: /files/** -> file:{}", defaultPath);
                registry.addResourceHandler("/files/**")
                        .addResourceLocations("file:" + defaultPath);
            }
        } catch (Exception e) {
            log.error("加载文件资源映射失败", e);
            // Fallback
            registry.addResourceHandler("/files/**")
                    .addResourceLocations("file:" + defaultBasePath());
        }
    }

    private String defaultBasePath() {
        String path = System.getenv().getOrDefault("LINGNOW_FILE_BASE_PATH", DEFAULT_FILE_BASE_PATH);
        return path.endsWith("/") ? path : path + "/";
    }
}
