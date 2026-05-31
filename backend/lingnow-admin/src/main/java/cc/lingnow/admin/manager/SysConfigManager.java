package cc.lingnow.admin.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cc.lingnow.admin.model.bo.ConfigQueryBO;
import cc.lingnow.admin.model.bo.ConfigSaveBO;
import cc.lingnow.admin.model.vo.ConfigVO;
import cc.lingnow.biz.config.entity.SysConfig;
import cc.lingnow.biz.config.service.SysConfigService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 参数配置管理业务逻辑
 *
 * @author LingNow Team
 */
@Service
@RequiredArgsConstructor
public class SysConfigManager {

    private final SysConfigService configService;

    /**
     * 查询参数配置列表
     */
    public PageResult<ConfigVO> listConfigs(ConfigQueryBO query) {
        // Similar pagination logic as Dict
        SysConfig config = BeanUtil.copyProperties(query, SysConfig.class);
        List<SysConfig> list = configService.selectConfigList(config);

        long total = list.size(); // Placeholder for actual pagination if service updated
        List<ConfigVO> voList = list.stream()
                .map(item -> BeanUtil.copyProperties(item, ConfigVO.class))
                .collect(Collectors.toList());

        return PageResult.of(query.getCurrent(), query.getSize(), total, voList);
    }

    /**
     * 查询参数配置详情
     */
    public ConfigVO getConfig(Long configId) {
        SysConfig config = configService.getById(configId);
        if (config == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return BeanUtil.copyProperties(config, ConfigVO.class);
    }

    /**
     * 新增参数配置
     */
    public void addConfig(ConfigSaveBO bo) {
        SysConfig config = BeanUtil.copyProperties(bo, SysConfig.class);
        if (!configService.checkConfigKeyUnique(config)) {
            throw new BusinessException(ErrorCode.CONFIG_KEY_EXIST);
        }
        configService.save(config);
    }

    /**
     * 修改参数配置
     */
    public void updateConfig(ConfigSaveBO bo) {
        if (ObjectUtil.isNull(bo.getConfigId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        SysConfig config = BeanUtil.copyProperties(bo, SysConfig.class);
        if (!configService.checkConfigKeyUnique(config)) {
            throw new BusinessException(ErrorCode.CONFIG_KEY_EXIST);
        }
        configService.updateById(config);
    }

    /**
     * 删除参数配置
     */
    public void removeConfig(List<Long> configIds) {
        configService.removeByIds(configIds);
    }

    /**
     * 刷新参数缓存。
     *
     * 当前基座未启用参数缓存，保留接口用于匹配管理端按钮能力。
     */
    public void refreshConfigCache() {
        // no-op
    }
}
