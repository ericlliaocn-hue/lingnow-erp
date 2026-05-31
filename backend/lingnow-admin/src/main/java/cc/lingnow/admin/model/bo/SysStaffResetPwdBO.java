package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 员工重置密码参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "员工重置密码参数")
public class SysStaffResetPwdBO {

    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;
}
