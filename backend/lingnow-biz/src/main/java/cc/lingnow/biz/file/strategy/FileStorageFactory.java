package cc.lingnow.biz.file.strategy;

import cc.lingnow.biz.file.entity.SysFileConfig;
import cc.lingnow.biz.file.mapper.SysFileConfigMapper;
import cc.lingnow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件存储工厂
 *
 * @author LingNow Team
 */
@Component
@RequiredArgsConstructor
public class FileStorageFactory {

    private final Map<String, FileStorageStrategy> strategyMap = new ConcurrentHashMap<>();
    private final List<FileStorageStrategy> strategies;
    private final SysFileConfigMapper fileConfigMapper;

    /**
     * 初始化策略映射
     */
    public void init() {
        for (FileStorageStrategy strategy : strategies) {
            strategyMap.put(strategy.getPlatform(), strategy);
        }
    }

    /**
     * 获取当前启用的存储策略
     *
     * @return 存储策略
     */
    public FileStorageStrategy getStrategy() {
        if (strategyMap.isEmpty()) {
            init();
        }

        // 查询当前启用的配置
        SysFileConfig config = fileConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysFileConfig>()
                        .eq(SysFileConfig::getIsActive, 1)
                        .last("LIMIT 1")
        );

        String platform = "LOCAL"; // 默认本地
        if (config != null) {
            platform = config.getPlatform();
        }

        FileStorageStrategy strategy = strategyMap.get(platform);
        if (strategy == null) {
            throw new BusinessException("未找到存储策略: " + platform);
        }
        return strategy;
    }

    /**
     * 获取指定平台的策略
     */
    public FileStorageStrategy getStrategy(String platform) {
        if (strategyMap.isEmpty()) {
            init();
        }
        return strategyMap.get(platform);
    }
}
