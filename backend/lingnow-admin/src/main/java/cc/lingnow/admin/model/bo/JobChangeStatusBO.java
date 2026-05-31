package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 定时任务状态切换参数
 */
@Data
@Schema(description = "定时任务状态切换参数")
public class JobChangeStatusBO {

    @Schema(description = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long jobId;

    @Schema(description = "状态 1正常 0暂停")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
