package cc.lingnow.biz.job.service.impl;

import cc.lingnow.biz.job.entity.SysJob;
import cc.lingnow.biz.job.mapper.SysJobMapper;
import cc.lingnow.biz.job.service.SysJobService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 定时任务 Service 实现
 */
@Service
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJob> implements SysJobService {
}
