package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 定时任务查询参数
 */
@Data
@Schema(description = "定时任务查询参数")
public class JobQueryBO {

    @Schema(description = "页码")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Long current = 1L;

    @Schema(description = "每页大小")
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    private Long size = 10L;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务组")
    private String jobGroup;

    @Schema(description = "状态")
    private Integer status;
}
