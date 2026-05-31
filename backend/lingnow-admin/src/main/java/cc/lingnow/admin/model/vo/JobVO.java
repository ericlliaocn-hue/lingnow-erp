package cc.lingnow.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务 VO
 */
@Data
@Schema(description = "定时任务VO")
public class JobVO {

    private Long jobId;

    private String jobName;

    private String jobGroup;

    private String invokeTarget;

    private String cronExpression;

    private String misfirePolicy;

    private String concurrent;

    private Integer status;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
