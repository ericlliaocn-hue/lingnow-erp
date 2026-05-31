package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 字典数据保存参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "字典数据保存参数")
public class DictDataSaveBO {

    @Schema(description = "字典编码 (更新时必填)")
    private Long dictCode;

    @Schema(description = "字典排序")
    @NotNull(message = "字典排序不能为空")
    private Integer dictSort;

    @Schema(description = "字典标签")
    @NotBlank(message = "字典标签不能为空")
    private String dictLabel;

    @Schema(description = "字典键值")
    @NotBlank(message = "字典键值不能为空")
    private String dictValue;

    @Schema(description = "字典类型")
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    @Schema(description = "样式属性")
    private String cssClass;

    @Schema(description = "表格回显样式")
    private String listClass;

    @Schema(description = "是否默认")
    private String isDefault;

    @Schema(description = "状态 (1正常 0停用)")
    private Boolean status;

    @Schema(description = "备注")
    private String remark;
}
