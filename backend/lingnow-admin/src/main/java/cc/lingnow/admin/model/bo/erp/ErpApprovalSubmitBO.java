package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ErpApprovalSubmitBO {
    @NotBlank(message = "业务类型不能为空")
    private String bizType;
    @NotNull(message = "业务ID不能为空")
    private Long bizId;
    private String comment;
}
