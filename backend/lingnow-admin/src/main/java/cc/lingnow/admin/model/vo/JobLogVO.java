package cc.lingnow.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务日志 VO
 */
@Data
@Schema(description = "定时任务日志VO")
public class JobLogVO {

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
