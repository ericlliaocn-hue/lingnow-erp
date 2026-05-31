package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 员工保存参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "员工保存参数")
public class SysStaffSaveBO {

    @Schema(description = "用户ID (更新时必填)")
    private Long userId;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "昵称")
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别 (0男 1女 2未知)")
    private Integer gender;

    @Schema(description = "状态 (0正常 1停用)")
    private Integer status;

    @Schema(description = "岗位ID组")
    private List<Long> postIds;

    @Schema(description = "角色ID组")
    private List<Long> roleIds;

    @Schema(description = "备注")
    private String remark;
}
