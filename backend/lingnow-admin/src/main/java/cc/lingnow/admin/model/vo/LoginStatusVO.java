package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录状态VO
 *
 * @author LingNow Team
 */
@Data
@Builder
@Schema(description = "登录状态信息")
public class LoginStatusVO {

    @Schema(description = "是否登录")
    private Boolean isLogin;

    @Schema(description = "管理员ID")
    private Long adminId;

    @Schema(description = "Token值")
    private String tokenValue;
}
