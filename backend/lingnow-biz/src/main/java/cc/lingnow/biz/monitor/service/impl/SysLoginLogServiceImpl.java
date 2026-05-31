package cc.lingnow.biz.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.monitor.entity.SysLoginLog;
import cc.lingnow.biz.monitor.mapper.SysLoginLogMapper;
import cc.lingnow.biz.monitor.service.SysLoginLogService;
import org.springframework.stereotype.Service;

/**
 * 系统登录日志服务实现
 *
 * @author LingNow Team
 */
@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements SysLoginLogService {
}
