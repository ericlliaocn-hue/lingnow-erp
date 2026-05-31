package cc.lingnow.biz.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cc.lingnow.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户系统通知实体
 *
 * @author LingNow Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_notification")
@Schema(description = "用户系统通知实体")
public class SysUserNotification extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "接收用户ID")
    private Long userId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "类型(info/success/warning/error)")
    private String type;

    @Schema(description = "是否已读(0否 1是)")
    private Integer isRead;

    @Schema(description = "业务ID")
    private Long bizId;

    @Schema(description = "业务类型")
    private String bizType;
}
