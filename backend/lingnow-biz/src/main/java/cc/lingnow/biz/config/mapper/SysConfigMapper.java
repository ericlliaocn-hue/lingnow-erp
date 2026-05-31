package cc.lingnow.biz.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.config.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参数配置Mapper接口
 *
 * @author LingNow Team
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
