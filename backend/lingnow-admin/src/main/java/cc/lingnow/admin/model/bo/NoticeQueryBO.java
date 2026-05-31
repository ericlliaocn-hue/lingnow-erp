package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通知公告查询参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "通知公告查询参数")
public class NoticeQueryBO {

    @Schema(description = "页码")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Long current = 1L;

    @Schema(description = "每页大小")
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    private Long size = 10L;

    @Schema(description = "公告标题")
    private String noticeTitle;

    @Schema(description = "操作人员")
    private String createBy;

    @Schema(description = "公告类型（1通知 2公告）")
    private String noticeType;

    @Schema(description = "公告状态（1正常 0关闭）")
    private Integer status;
}
