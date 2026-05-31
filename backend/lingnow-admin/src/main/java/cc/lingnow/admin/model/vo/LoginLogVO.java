package cc.lingnow.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志 VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "登录日志信息")
public class LoginLogVO {

    @Schema(description = "访问ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long infoId;

    @Schema(description = "用户账号")
    private String userName;

    @Schema(description = "登录IP地址")
    private String ipaddr;

    @Schema(description = "登录地点")
    private String loginLocation;

    @Schema(description = "浏览器类型")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录状态（0成功 1失败）")
    private Integer status;

    @Schema(description = "提示消息")
    private String msg;

    @Schema(description = "访问时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;
}
