package cc.lingnow.biz.app.entity;

import cc.lingnow.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * App用户扩展信息表
 *
 * @author LingNow Team
 */
@Data
@TableName("app_user_info")
@Schema(description = "App用户扩展信息实体")
public class AppUserInfo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID (主键)
     */
    @Schema(description = "用户ID")
    @TableId(type = IdType.INPUT)
    private Long userId;

    /**
     * 性别 (0-女 1-男 2-其他)
     */
    @Schema(description = "性别")
    private Integer gender;

    /**
     * 生日
     */
    @Schema(description = "生日")
    private LocalDate birthday;

    /**
     * 所在地区
     */
    @Schema(description = "所在地区")
    private String region;

    /**
     * 个性签名
     */
    @Schema(description = "个性签名")
    private String signature;

    /**
     * 标签 (JSON存储)
     */
    @Schema(description = "标签")
    private String tags;
}
