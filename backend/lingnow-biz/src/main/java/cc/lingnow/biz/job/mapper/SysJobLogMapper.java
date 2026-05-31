package cc.lingnow.biz.job.mapper;

import cc.lingnow.biz.job.entity.SysJobLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务日志 Mapper
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {
}
