package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ErpCustomerAccountSaveBO {
    private Long id;
    @NotNull(message = "客户不能为空")
    private Long customerId;
    @NotBlank(message = "账号不能为空")
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private Integer status;
    private String remark;
}
