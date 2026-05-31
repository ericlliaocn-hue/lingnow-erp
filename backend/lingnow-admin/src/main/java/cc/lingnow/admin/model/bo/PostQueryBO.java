package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位查询参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "岗位查询参数")
public class PostQueryBO {

    @Schema(description = "岗位编码")
    private String postCode;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "状态 (1正常 0停用)")
    private Integer status;

    @Schema(description = "部门ID")
    private Long deptId;
}
