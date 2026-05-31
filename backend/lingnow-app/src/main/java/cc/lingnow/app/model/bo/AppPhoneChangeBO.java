package cc.lingnow.app.model.bo;

import cc.lingnow.common.annotation.Mobile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改手机号参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "修改手机号参数")
public class AppPhoneChangeBO {

    @Schema(description = "原手机号验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原手机号验证码不能为空")
    private String oldCode;

    @Schema(description = "新手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新手机号不能为空")
    @Mobile
    private String newPhone;

    @Schema(description = "新手机号验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新手机号验证码不能为空")
    private String newCode;
}
