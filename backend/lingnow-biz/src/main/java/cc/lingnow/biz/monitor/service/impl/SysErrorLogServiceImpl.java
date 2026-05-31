package cc.lingnow.biz.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.monitor.entity.SysErrorLog;
import cc.lingnow.biz.monitor.mapper.SysErrorLogMapper;
import cc.lingnow.biz.monitor.service.SysErrorLogService;
import org.springframework.stereotype.Service;

/**
 * 错误日志服务实现
 *
 * @author LingNow Team
 */
@Service
public class SysErrorLogServiceImpl extends ServiceImpl<SysErrorLogMapper, SysErrorLog> implements SysErrorLogService {
}
