package cc.lingnow.admin.listener;

import cc.lingnow.biz.monitor.entity.SysErrorLog;
import cc.lingnow.biz.monitor.entity.SysSlowSqlLog;
import cc.lingnow.biz.monitor.service.SysErrorLogService;
import cc.lingnow.biz.monitor.service.SysSlowSqlLogService;
import cc.lingnow.common.event.ErrorLogEvent;
import cc.lingnow.common.event.SlowSqlLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 日志事件监听器
 *
 * @author LingNow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventListener {

    private final SysErrorLogService sysErrorLogService;
    private final SysSlowSqlLogService sysSlowSqlLogService;

    /**
     * 监听错误日志事件
     */
    @Async
    @EventListener
    public void handleErrorLogEvent(ErrorLogEvent event) {
        try {
            SysErrorLog errorLog = new SysErrorLog();
            errorLog.setTraceId(UUID.randomUUID().toString()); // 简单生成一个TraceId
            errorLog.setUserId(event.getUserId());
            errorLog.setUserName(event.getUserName());
            errorLog.setRequestMethod(event.getRequestMethod());
            errorLog.setRequestUrl(event.getRequestUrl());
            errorLog.setRequestParams(event.getRequestParams());
            errorLog.setIp(event.getIp());
            errorLog.setErrorMsg(event.getErrorMsg());
            errorLog.setErrorStack(event.getErrorStack());
            errorLog.setCreateTime(LocalDateTime.now());

            sysErrorLogService.save(errorLog);
        } catch (Exception e) {
            log.error("保存错误日志失败", e);
        }
    }

    /**
     * 监听慢SQL日志事件
     */
    @Async
    @EventListener
    public void handleSlowSqlLogEvent(SlowSqlLogEvent event) {
        try {
            SysSlowSqlLog slowSqlLog = new SysSlowSqlLog();
            slowSqlLog.setTraceId(UUID.randomUUID().toString());
            slowSqlLog.setUserId(event.getUserId());
            slowSqlLog.setUserName(event.getUserName());
            slowSqlLog.setExecutionTime(event.getExecutionTime());
            slowSqlLog.setSqlStatement(event.getSqlStatement());
            slowSqlLog.setCreateTime(LocalDateTime.now());

            sysSlowSqlLogService.save(slowSqlLog);
        } catch (Exception e) {
            log.error("保存慢SQL日志失败", e);
        }
    }
}
