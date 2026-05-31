package cc.lingnow.admin.model.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户状态 BO
 *
 * @author LingNow Team
 */
@Data
public class UserStatusBO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 状态
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 原因/备注（可选）
     */
    private String reason;

}
