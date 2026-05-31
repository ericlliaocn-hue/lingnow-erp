package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 字典类型查询参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "字典类型查询参数")
public class DictTypeQueryBO {

    @Schema(description = "页码")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Long current = 1L;

    @Schema(description = "每页大小")
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    private Long size = 10L;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "状态 (1正常 0停用)")
    private Integer status;
}
