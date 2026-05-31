package cc.lingnow.biz.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.monitor.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统登录日志 Mapper
 *
 * @author LingNow Team
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}
