package cc.lingnow.biz.job.service.impl;

import cc.lingnow.biz.job.entity.SysJobLog;
import cc.lingnow.biz.job.mapper.SysJobLogMapper;
import cc.lingnow.biz.job.service.SysJobLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 定时任务日志 Service 实现
 */
@Service
public class SysJobLogServiceImpl extends ServiceImpl<SysJobLogMapper, SysJobLog> implements SysJobLogService {
}
