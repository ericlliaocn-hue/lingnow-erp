package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "字典数据信息VO")
public class DictDataVO {

    @Schema(description = "字典编码")
    private Long dictCode;

    @Schema(description = "字典排序")
    private Integer dictSort;

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "字典键值")
    private String dictValue;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "样式属性")
    private String cssClass;

    @Schema(description = "表格回显样式")
    private String listClass;

    @Schema(description = "是否默认")
    private String isDefault;

    @Schema(description = "状态 (1正常 0停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
