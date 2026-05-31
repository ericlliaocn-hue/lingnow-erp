package cc.lingnow.biz.user.entity;

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
 * 社交账号绑定实体
 *
 * @author LingNow Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_social_user")
@Schema(description = "社交账号绑定实体")
public class SysSocialUser extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 第三方平台 (wechat, alipay, weibo, xiaohongshu, douyin)
     */
    @Schema(description = "第三方平台")
    private String provider;

    /**
     * 平台OpenID
     */
    @Schema(description = "平台OpenID")
    private String openId;

    /**
     * 平台UnionID
     */
    @Schema(description = "平台UnionID")
    private String unionId;

    /**
     * 平台昵称
     */
    @Schema(description = "平台昵称")
    private String nickname;

    /**
     * 平台头像
     */
    @Schema(description = "平台头像")
    private String avatar;
}
