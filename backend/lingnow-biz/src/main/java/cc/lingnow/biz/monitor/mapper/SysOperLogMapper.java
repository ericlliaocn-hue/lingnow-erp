package cc.lingnow.biz.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.monitor.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志记录 Mapper
 *
 * @author LingNow Team
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
