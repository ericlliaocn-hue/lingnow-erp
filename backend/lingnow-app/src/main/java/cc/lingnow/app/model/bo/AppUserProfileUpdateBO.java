package cc.lingnow.app.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改个人资料参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "修改个人资料参数")
public class AppUserProfileUpdateBO {

    @Schema(description = "头像", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String avatar;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nickname;

    @Schema(description = "性别 (0-女 1-男 2-其他)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer gender;

    @Schema(description = "生日", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String birthday;

    @Schema(description = "地区", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String region;
}
