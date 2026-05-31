package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "岗位信息VO")
public class PostVO {

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "岗位编码")
    private String postCode;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "显示顺序")
    private Integer postSort;

    @Schema(description = "状态 (1正常 0停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
