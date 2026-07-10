package cc.lingnow.admin.model.bo.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopRegisterBO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名不能超过64个字符")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为6-32位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
