package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 岗位保存参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "岗位保存参数")
public class PostSaveBO {

    @Schema(description = "岗位ID (更新时必填)")
    private Long postId;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位编码")
    @NotBlank(message = "岗位编码不能为空")
    private String postCode;

    @Schema(description = "岗位名称")
    @NotBlank(message = "岗位名称不能为空")
    private String postName;

    @Schema(description = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    private Integer postSort;

    @Schema(description = "状态 (1正常 0停用)")
    private Boolean status;

    @Schema(description = "备注")
    private String remark;
}
