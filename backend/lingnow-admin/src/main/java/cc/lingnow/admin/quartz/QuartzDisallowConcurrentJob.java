package cc.lingnow.admin.quartz;

import cc.lingnow.biz.job.service.SysJobLogService;
import org.quartz.DisallowConcurrentExecution;

/**
 * 禁止并发执行的 Quartz 任务。
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentJob extends QuartzJob {
}
