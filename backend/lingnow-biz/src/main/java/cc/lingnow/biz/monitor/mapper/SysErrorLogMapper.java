package cc.lingnow.biz.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.monitor.entity.SysErrorLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错误日志 Mapper
 *
 * @author LingNow Team
 */
@Mapper
public interface SysErrorLogMapper extends BaseMapper<SysErrorLog> {
}
