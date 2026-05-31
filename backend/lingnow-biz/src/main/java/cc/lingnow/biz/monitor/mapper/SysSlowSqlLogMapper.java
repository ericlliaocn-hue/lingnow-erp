package cc.lingnow.biz.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.monitor.entity.SysSlowSqlLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 慢SQL日志 Mapper
 *
 * @author LingNow Team
 */
@Mapper
public interface SysSlowSqlLogMapper extends BaseMapper<SysSlowSqlLog> {
}
