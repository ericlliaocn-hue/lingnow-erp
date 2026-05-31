package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "部门信息VO")
public class DeptVO {

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "父部门ID")
    private Long parentId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "显示顺序")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
