package cc.lingnow.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import cc.lingnow.biz.role.entity.SysRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情 VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "用户详细信息")
public class UserDetailVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别 (0-女 1-男 2-其他)")
    private Integer gender;

    @Schema(description = "生日")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Schema(description = "所在地区")
    private String region;

    @Schema(description = "状态 (0-正常 1-禁用)")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    private String createBy;

    @Schema(description = "更新人ID")
    private String updateBy;

    @Schema(description = "角色列表")
    private List<SysRole> roles;

}
