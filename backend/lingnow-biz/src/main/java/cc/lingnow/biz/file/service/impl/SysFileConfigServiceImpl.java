package cc.lingnow.biz.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.file.entity.SysFileConfig;
import cc.lingnow.biz.file.mapper.SysFileConfigMapper;
import cc.lingnow.biz.file.service.SysFileConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件配置服务实现类
 *
 * @author LingNow Team
 */
@Service
public class SysFileConfigServiceImpl extends ServiceImpl<SysFileConfigMapper, SysFileConfig> implements SysFileConfigService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SysFileConfig config) {
        if (config.getIsActive() != null && config.getIsActive() == 1) {
            // 如果设置为激活，则先将其他配置设为未激活
            // 使用 lambdaUpdate() 链式调用，避免 update(entity, wrapper) 可能引起的参数映射问题
            this.lambdaUpdate()
                    .set(SysFileConfig::getIsActive, 0)
                    .eq(SysFileConfig::getIsActive, 1)
                    .update();
        }

        if (config.getId() != null) {
            this.updateById(config);
        } else {
            this.save(config);
        }
    }
}
