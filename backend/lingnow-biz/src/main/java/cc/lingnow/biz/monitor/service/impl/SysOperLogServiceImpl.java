package cc.lingnow.biz.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.monitor.entity.SysOperLog;
import cc.lingnow.biz.monitor.mapper.SysOperLogMapper;
import cc.lingnow.biz.monitor.service.SysOperLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 *
 * @author LingNow Team
 */
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {
}
