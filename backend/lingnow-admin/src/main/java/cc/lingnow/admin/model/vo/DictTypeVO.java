package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "字典类型信息VO")
public class DictTypeVO {

    @Schema(description = "字典主键")
    private Long dictId;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "状态 (1正常 0停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
