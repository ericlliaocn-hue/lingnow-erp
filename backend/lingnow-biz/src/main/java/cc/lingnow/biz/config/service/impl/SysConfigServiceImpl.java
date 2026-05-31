package cc.lingnow.biz.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.config.entity.SysConfig;
import cc.lingnow.biz.config.mapper.SysConfigMapper;
import cc.lingnow.biz.config.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 参数配置Service业务层处理
 *
 * @author LingNow Team
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public List<SysConfig> selectConfigList(SysConfig config) {
        LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<>();
        lqw.like(ObjectUtils.isNotEmpty(config.getConfigName()), SysConfig::getConfigName, config.getConfigName())
                .eq(ObjectUtils.isNotEmpty(config.getConfigType()), SysConfig::getConfigType, config.getConfigType())
                .like(ObjectUtils.isNotEmpty(config.getConfigKey()), SysConfig::getConfigKey, config.getConfigKey())
                .eq(SysConfig::getDelFlag, false)
                .orderByDesc(SysConfig::getCreateTime);
        return baseMapper.selectList(lqw);
    }

    @Override
    public boolean checkConfigKeyUnique(SysConfig config) {
        Long configId = ObjectUtils.isEmpty(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, config.getConfigKey())
                .last("limit 1"));
        return ObjectUtils.isEmpty(info) || info.getConfigId().longValue() == configId.longValue();
    }

    @Override
    public String selectConfigByKey(String configKey) {
        SysConfig config = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey));
        return ObjectUtils.isNotEmpty(config) ? config.getConfigValue() : "";
    }
}
