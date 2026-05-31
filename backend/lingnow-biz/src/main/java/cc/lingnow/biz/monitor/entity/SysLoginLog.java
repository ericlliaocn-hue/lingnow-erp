package cc.lingnow.biz.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cc.lingnow.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统登录日志
 *
 * @author LingNow Team
 */
@Data
@TableName("sys_login_log")
@Schema(description = "系统登录日志")
public class SysLoginLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "访问ID")
    private Long infoId;

    /**
     * 用户账号
     */
    @Schema(description = "用户账号")
    private String userName;

    /**
     * 登录IP地址
     */
    @Schema(description = "登录IP地址")
    private String ipaddr;

    /**
     * 登录地点
     */
    @Schema(description = "登录地点")
    private String loginLocation;

    /**
     * 浏览器类型
     */
    @Schema(description = "浏览器类型")
    private String browser;

    /**
     * 操作系统
     */
    @Schema(description = "操作系统")
    private String os;

    /**
     * 登录状态（1成功 0失败）
     */
    @Schema(description = "登录状态（1成功 0失败）")
    private Integer status;

    /**
     * 提示消息
     */
    @Schema(description = "提示消息")
    private String msg;

    /**
     * 访问时间
     */
    @Schema(description = "访问时间")
    private LocalDateTime loginTime;
}
