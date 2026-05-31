package cc.lingnow.biz.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.file.entity.SysFileConfig;

/**
 * 文件配置服务接口
 *
 * @author LingNow Team
 */
public interface SysFileConfigService extends IService<SysFileConfig> {

    /**
     * 更新配置并激活
     *
     * @param config 配置
     */
    void updateConfig(SysFileConfig config);
}
