package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 慢SQL日志 VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "慢SQL日志 VO")
public class SlowSqlLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "追踪ID")
    private String traceId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "执行时长(ms)")
    private Long executionTime;

    @Schema(description = "SQL语句")
    private String sqlStatement;

    @Schema(description = "创建时间")
    private Date createTime;
}
