package cc.lingnow.admin.model.bo;

import cc.lingnow.common.vo.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 慢SQL日志查询参数
 *
 * @author LingNow Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "慢SQL日志查询参数")
public class SlowSqlLogQueryBO extends PageResult {

    public SlowSqlLogQueryBO() {
        setCurrent(1L);
        setSize(10L);
    }

    @Schema(description = "追踪ID")
    private String traceId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "最小执行时长")
    private Long minExecutionTime;
}
