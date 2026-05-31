package cc.lingnow.biz.job.mapper;

import cc.lingnow.biz.job.entity.SysJob;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务 Mapper
 */
@Mapper
public interface SysJobMapper extends BaseMapper<SysJob> {
}
