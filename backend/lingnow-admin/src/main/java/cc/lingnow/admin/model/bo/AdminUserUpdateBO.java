package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端用户更新 BO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "管理端用户更新参数")
public class AdminUserUpdateBO {

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    @Schema(description = "邮箱")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "头像地址")
    @Size(max = 255, message = "头像地址长度不能超过255个字符")
    private String avatar;

    @Schema(description = "性别 (0-女 1-男 2-其他)")
    private Integer gender;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "所在地区")
    @Size(max = 100, message = "地区长度不能超过100个字符")
    private String region;

    @Schema(description = "状态 (0-正常 1-禁用)")
    private Integer status;
}
