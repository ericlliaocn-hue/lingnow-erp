package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户统计 VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "用户统计信息")
public class UserStatsVO {

    @Schema(description = "用户总数")
    private Long totalUsers;

    @Schema(description = "禁用用户数量")
    private Long disabledUsers;

    @Schema(description = "今日新增用户数量")
    private Long todayNewUsers;
}
