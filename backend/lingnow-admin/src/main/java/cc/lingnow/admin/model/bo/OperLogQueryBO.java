package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查询 BO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "操作日志查询参数")
public class OperLogQueryBO {

    @Schema(description = "页码")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Long current = 1L;

    @Schema(description = "每页大小")
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    private Long size = 10L;

    @Schema(description = "模块标题")
    private String title;

    @Schema(description = "操作人员")
    private String operName;

    @Schema(description = "业务类型（0其它 1新增 2修改 3删除）")
    private Integer businessType;

    @Schema(description = "操作状态（0正常 1异常）")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
