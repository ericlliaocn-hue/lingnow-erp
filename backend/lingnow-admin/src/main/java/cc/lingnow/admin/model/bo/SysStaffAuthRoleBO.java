package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 员工分配角色参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "员工分配角色参数")
public class SysStaffAuthRoleBO {

    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "角色ID组")
    private List<Long> roleIds;
}
