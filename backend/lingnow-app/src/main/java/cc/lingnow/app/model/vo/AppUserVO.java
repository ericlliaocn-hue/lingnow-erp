package cc.lingnow.app.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * APP用户VO
 *
 * @author LingNow Team
 */
@Data
@Builder
@Schema(description = "APP用户VO")
public class AppUserVO {

    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号 (已脱敏)")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "Token")
    private String token;

}
