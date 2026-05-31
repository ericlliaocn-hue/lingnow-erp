package cc.lingnow.admin.model.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户查询 BO
 *
 * @author LingNow Team
 */
@Data
public class UserQueryBO {

    /**
     * 当前页码
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Long current = 1L;

    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    private Long size = 10L;

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 手机号（模糊查询）
     */
    private String phone;

    /**
     * 状态
     */
    private Integer status;

}
