package cc.lingnow.admin.quartz;

import cc.lingnow.biz.job.entity.SysJob;
import cc.lingnow.biz.job.entity.SysJobLog;
import cc.lingnow.biz.job.service.SysJobLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * Quartz 任务执行器。
 */
@Slf4j
public class QuartzJob implements Job {

    @Autowired
    private JobInvokeHelper invokeHelper;

    @Autowired
    private SysJobLogService jobLogService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SysJob job = (SysJob) context.getMergedJobDataMap().get(JobConstants.JOB_DATA_KEY);
        executeJob(job);
    }

    protected void executeJob(SysJob job) throws JobExecutionException {
        if (job == null) {
            return;
        }
        LocalDateTime startTime = LocalDateTime.now();
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobId(job.getJobId());
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setStartTime(startTime);

        try {
            invokeHelper.invoke(job.getInvokeTarget());
            LocalDateTime endTime = LocalDateTime.now();
            jobLog.setStatus(1);
            jobLog.setJobMessage("任务执行成功");
            jobLog.setEndTime(endTime);
            jobLog.setDurationMs(java.time.Duration.between(startTime, endTime).toMillis());
            jobLogService.save(jobLog);
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            jobLog.setStatus(0);
            jobLog.setJobMessage("任务执行失败");
            jobLog.setExceptionInfo(e.getMessage());
            jobLog.setEndTime(endTime);
            jobLog.setDurationMs(java.time.Duration.between(startTime, endTime).toMillis());
            jobLogService.save(jobLog);
            log.error("定时任务执行失败: jobId={}, target={}", job.getJobId(), job.getInvokeTarget(), e);
            throw new JobExecutionException(e);
        }
    }
}
