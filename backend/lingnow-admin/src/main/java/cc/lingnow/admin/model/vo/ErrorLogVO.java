package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 错误日志 VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "错误日志 VO")
public class ErrorLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "追踪ID")
    private String traceId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "请求URL")
    private String requestUrl;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "堆栈信息")
    private String errorStack;

    @Schema(description = "创建时间")
    private Date createTime;
}
