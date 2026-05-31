package cc.lingnow.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import cc.lingnow.biz.dept.entity.SysDept;
import cc.lingnow.biz.post.entity.SysPost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工信息VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "员工信息VO")
public class SysStaffVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "部门对象")
    private SysDept dept;

    @Schema(description = "岗位组")
    private List<SysPost> posts;

    @Schema(description = "角色组")
    private List<Long> roleIds;

    @Schema(description = "岗位ID组")
    private List<Long> postIds;
}
