package cc.lingnow.biz.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.config.entity.SysConfig;

import java.util.List;

/**
 * 参数配置Service接口
 *
 * @author LingNow Team
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 查询参数配置列表
     *
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    List<SysConfig> selectConfigList(SysConfig config);

    /**
     * 校验参数键名是否唯一
     *
     * @param config 参数配置信息
     * @return 结果
     */
    boolean checkConfigKeyUnique(SysConfig config);

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数键名
     * @return 参数键值
     */
    String selectConfigByKey(String configKey);
}
