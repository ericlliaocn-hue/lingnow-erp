package cc.lingnow.admin.model.bo.shop;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopLoginBO {
    @NotBlank(message = "账号不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}
