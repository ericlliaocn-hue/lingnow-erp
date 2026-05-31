package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 部门保存参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "部门保存参数")
public class DeptSaveBO {

    @Schema(description = "部门ID (更新时必填)")
    private Long deptId;

    @Schema(description = "父部门ID")
    private Long parentId;

    @Schema(description = "部门名称")
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    @Schema(description = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    private Integer orderNum;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "类别编码")
    private String categoryCode;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "部门状态 (1正常 0停用)")
    private Integer status;
}
