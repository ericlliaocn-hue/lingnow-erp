package cc.lingnow.biz.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.monitor.entity.SysSlowSqlLog;
import cc.lingnow.biz.monitor.mapper.SysSlowSqlLogMapper;
import cc.lingnow.biz.monitor.service.SysSlowSqlLogService;
import org.springframework.stereotype.Service;

/**
 * 慢SQL日志服务实现
 *
 * @author LingNow Team
 */
@Service
public class SysSlowSqlLogServiceImpl extends ServiceImpl<SysSlowSqlLogMapper, SysSlowSqlLog> implements SysSlowSqlLogService {
}
