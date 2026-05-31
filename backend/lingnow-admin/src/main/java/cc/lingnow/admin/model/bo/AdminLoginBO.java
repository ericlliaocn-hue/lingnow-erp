package cc.lingnow.admin.model.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员登录 BO
 *
 * @author LingNow Team
 */
@Data
public class AdminLoginBO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

}
