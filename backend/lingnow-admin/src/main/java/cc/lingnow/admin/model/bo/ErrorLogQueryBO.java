package cc.lingnow.admin.model.bo;

import cc.lingnow.common.vo.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 错误日志查询参数
 *
 * @author LingNow Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "错误日志查询参数")
public class ErrorLogQueryBO extends PageResult {

    public ErrorLogQueryBO() {
        setCurrent(1L);
        setSize(10L);
    }

    @Schema(description = "追踪ID")
    private String traceId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "请求URL")
    private String requestUrl;
}
