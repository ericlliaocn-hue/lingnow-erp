package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部门查询参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "部门查询参数")
public class DeptQueryBO {

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门状态 (1正常 0停用)")
    private Integer status;
}
