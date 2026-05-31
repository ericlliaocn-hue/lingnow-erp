package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理员登录结果 VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "管理员登录结果")
public class AdminLoginVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "权限列表")
    private java.util.List<String> permissions;
}
