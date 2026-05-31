package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典类型保存参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "字典类型保存参数")
public class DictTypeSaveBO {

    @Schema(description = "字典ID (更新时必填)")
    private Long dictId;

    @Schema(description = "字典名称")
    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    @Schema(description = "字典类型")
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    @Schema(description = "状态 (1正常 0停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
