package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 定时任务保存参数
 */
@Data
@Schema(description = "定时任务保存参数")
public class JobSaveBO {

    @Schema(description = "任务ID")
    private Long jobId;

    @Schema(description = "任务名称")
    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    @Schema(description = "任务组")
    @NotBlank(message = "任务组不能为空")
    private String jobGroup;

    @Schema(description = "调用目标")
    @NotBlank(message = "调用目标不能为空")
    private String invokeTarget;

    @Schema(description = "Cron表达式")
    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    @Schema(description = "错过执行策略")
    private String misfirePolicy = "DO_NOTHING";

    @Schema(description = "是否允许并发 Y/N")
    private String concurrent = "N";

    @Schema(description = "状态 1正常 0暂停")
    private Integer status = 1;

    @Schema(description = "备注")
    private String remark;
}
