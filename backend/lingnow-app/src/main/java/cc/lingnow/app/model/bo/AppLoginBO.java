package cc.lingnow.app.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * APP登录参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "APP登录参数")
public class AppLoginBO {

    @Schema(description = "手机号/账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "密码/验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "凭证不能为空")
    private String credential;

    @Schema(description = "登录类型 (password/code)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录类型不能为空")
    private String type;
}
