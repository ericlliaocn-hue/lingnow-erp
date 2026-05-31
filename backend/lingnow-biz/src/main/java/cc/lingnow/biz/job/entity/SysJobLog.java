package cc.lingnow.biz.job.entity;

import cc.lingnow.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务执行日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_log")
public class SysJobLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long jobLogId;

    private Long jobId;

    private String jobName;

    private String jobGroup;

    private String invokeTarget;

    private String jobMessage;

    private Integer status;

    private String exceptionInfo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long durationMs;
}
