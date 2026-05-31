package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件配置VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "文件配置VO")
public class FileConfigVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "平台")
    private String platform;

    @Schema(description = "配置信息(JSON)")
    private String configJson;

    @Schema(description = "是否启用")
    private Integer isActive;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
