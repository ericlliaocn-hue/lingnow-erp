package cc.lingnow.admin.manager;

import cc.lingnow.admin.model.bo.JobChangeStatusBO;
import cc.lingnow.admin.model.bo.JobLogQueryBO;
import cc.lingnow.admin.model.bo.JobQueryBO;
import cc.lingnow.admin.model.bo.JobSaveBO;
import cc.lingnow.admin.model.vo.JobLogVO;
import cc.lingnow.admin.model.vo.JobVO;
import cc.lingnow.admin.quartz.JobConstants;
import cc.lingnow.admin.quartz.JobInvokeHelper;
import cc.lingnow.admin.quartz.QuartzDisallowConcurrentJob;
import cc.lingnow.admin.quartz.QuartzJob;
import cc.lingnow.biz.job.entity.SysJob;
import cc.lingnow.biz.job.entity.SysJobLog;
import cc.lingnow.biz.job.service.SysJobLogService;
import cc.lingnow.biz.job.service.SysJobService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务管理业务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobManager {

    private final SysJobService jobService;
    private final SysJobLogService jobLogService;
    private final Scheduler scheduler;
    private final JobInvokeHelper invokeHelper;

    public PageResult<JobVO> listJobs(JobQueryBO query) {
        Page<SysJob> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<SysJob> wrapper = new LambdaQueryWrapper<SysJob>()
                .like(StrUtil.isNotBlank(query.getJobName()), SysJob::getJobName, query.getJobName())
                .eq(StrUtil.isNotBlank(query.getJobGroup()), SysJob::getJobGroup, query.getJobGroup())
                .eq(query.getStatus() != null, SysJob::getStatus, query.getStatus())
                .orderByDesc(SysJob::getCreateTime);
        IPage<SysJob> result = jobService.page(page, wrapper);
        List<JobVO> records = result.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, JobVO.class))
                .toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    public JobVO getJob(Long jobId) {
        SysJob job = getRequiredJob(jobId);
        return BeanUtil.copyProperties(job, JobVO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addJob(JobSaveBO bo) {
        SysJob job = BeanUtil.copyProperties(bo, SysJob.class);
        normalize(job);
        validateJob(job);
        jobService.save(job);
        if (JobConstants.STATUS_NORMAL.equals(job.getStatus())) {
            scheduleJob(job);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateJob(JobSaveBO bo) {
        if (bo.getJobId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        getRequiredJob(bo.getJobId());
        SysJob job = BeanUtil.copyProperties(bo, SysJob.class);
        normalize(job);
        validateJob(job);
        jobService.updateById(job);
        deleteScheduledJob(job.getJobId(), job.getJobGroup());
        if (JobConstants.STATUS_NORMAL.equals(job.getStatus())) {
            scheduleJob(job);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeJobs(List<Long> jobIds) {
        if (CollUtil.isEmpty(jobIds)) {
            return;
        }
        List<SysJob> jobs = jobService.listByIds(jobIds);
        for (SysJob job : jobs) {
            deleteScheduledJob(job.getJobId(), job.getJobGroup());
        }
        jobService.removeByIds(jobIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(JobChangeStatusBO bo) {
        SysJob job = getRequiredJob(bo.getJobId());
        if (!JobConstants.STATUS_NORMAL.equals(bo.getStatus()) && !JobConstants.STATUS_PAUSED.equals(bo.getStatus())) {
            throw new BusinessException("任务状态不合法");
        }
        job.setStatus(bo.getStatus());
        jobService.updateById(job);
        deleteScheduledJob(job.getJobId(), job.getJobGroup());
        if (JobConstants.STATUS_NORMAL.equals(job.getStatus())) {
            scheduleJob(job);
        }
    }

    public void runOnce(Long jobId) {
        SysJob job = getRequiredJob(jobId);
        validateJob(job);
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
            jobLog.setJobMessage("手动执行成功");
            jobLog.setEndTime(endTime);
            jobLog.setDurationMs(java.time.Duration.between(startTime, endTime).toMillis());
            jobLogService.save(jobLog);
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            jobLog.setStatus(0);
            jobLog.setJobMessage("手动执行失败");
            jobLog.setExceptionInfo(e.getMessage());
            jobLog.setEndTime(endTime);
            jobLog.setDurationMs(java.time.Duration.between(startTime, endTime).toMillis());
            jobLogService.save(jobLog);
            throw new BusinessException("任务执行失败：" + e.getMessage());
        }
    }

    public PageResult<JobLogVO> listJobLogs(JobLogQueryBO query) {
        Page<SysJobLog> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<SysJobLog>()
                .eq(query.getJobId() != null, SysJobLog::getJobId, query.getJobId())
                .like(StrUtil.isNotBlank(query.getJobName()), SysJobLog::getJobName, query.getJobName())
                .eq(StrUtil.isNotBlank(query.getJobGroup()), SysJobLog::getJobGroup, query.getJobGroup())
                .eq(query.getStatus() != null, SysJobLog::getStatus, query.getStatus())
                .orderByDesc(SysJobLog::getStartTime);
        IPage<SysJobLog> result = jobLogService.page(page, wrapper);
        List<JobLogVO> records = result.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, JobLogVO.class))
                .toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    public void removeJobLogs(List<Long> logIds) {
        if (CollUtil.isNotEmpty(logIds)) {
            jobLogService.removeByIds(logIds);
        }
    }

    public void cleanJobLogs() {
        jobLogService.remove(Wrappers.<SysJobLog>lambdaQuery().isNotNull(SysJobLog::getJobLogId));
    }

    public void scheduleEnabledJobs() {
        List<SysJob> jobs = jobService.list(new LambdaQueryWrapper<SysJob>()
                .eq(SysJob::getStatus, JobConstants.STATUS_NORMAL));
        for (SysJob job : jobs) {
            try {
                scheduleJob(job);
            } catch (Exception e) {
                log.error("注册定时任务失败: jobId={}, target={}", job.getJobId(), job.getInvokeTarget(), e);
            }
        }
    }

    private SysJob getRequiredJob(Long jobId) {
        SysJob job = jobService.getById(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return job;
    }

    private void normalize(SysJob job) {
        if (job.getStatus() == null) job.setStatus(JobConstants.STATUS_PAUSED);
        if (StrUtil.isBlank(job.getMisfirePolicy())) job.setMisfirePolicy(JobConstants.MISFIRE_DO_NOTHING);
        if (StrUtil.isBlank(job.getConcurrent())) job.setConcurrent(JobConstants.CONCURRENT_DISALLOWED);
    }

    private void validateJob(SysJob job) {
        if (!CronExpression.isValidExpression(job.getCronExpression())) {
            throw new BusinessException("Cron表达式不合法");
        }
        if (!JobConstants.CONCURRENT_ALLOWED.equals(job.getConcurrent())
                && !JobConstants.CONCURRENT_DISALLOWED.equals(job.getConcurrent())) {
            throw new BusinessException("并发策略不合法");
        }
        invokeHelper.validate(job.getInvokeTarget());
    }

    private void scheduleJob(SysJob job) {
        try {
            JobKey jobKey = getJobKey(job);
            TriggerKey triggerKey = getTriggerKey(job);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            JobDetail jobDetail = JobBuilder.newJob(getQuartzJobClass(job))
                    .withIdentity(jobKey)
                    .build();
            jobDetail.getJobDataMap().put(JobConstants.JOB_DATA_KEY, job);
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(buildSchedule(job))
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new BusinessException("注册定时任务失败：" + e.getMessage());
        }
    }

    private void deleteScheduledJob(Long jobId, String jobGroup) {
        try {
            JobKey jobKey = JobKey.jobKey("JOB_" + jobId, jobGroup);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            throw new BusinessException("移除定时任务失败：" + e.getMessage());
        }
    }

    private Class<? extends Job> getQuartzJobClass(SysJob job) {
        return JobConstants.CONCURRENT_ALLOWED.equals(job.getConcurrent())
                ? QuartzJob.class
                : QuartzDisallowConcurrentJob.class;
    }

    private JobKey getJobKey(SysJob job) {
        return JobKey.jobKey("JOB_" + job.getJobId(), job.getJobGroup());
    }

    private TriggerKey getTriggerKey(SysJob job) {
        return TriggerKey.triggerKey("TRIGGER_" + job.getJobId(), job.getJobGroup());
    }

    private CronScheduleBuilder buildSchedule(SysJob job) {
        CronScheduleBuilder builder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        return switch (job.getMisfirePolicy()) {
            case JobConstants.MISFIRE_IGNORE -> builder.withMisfireHandlingInstructionIgnoreMisfires();
            case JobConstants.MISFIRE_FIRE_ONCE -> builder.withMisfireHandlingInstructionFireAndProceed();
            case JobConstants.MISFIRE_DO_NOTHING -> builder.withMisfireHandlingInstructionDoNothing();
            default -> builder;
        };
    }
}
