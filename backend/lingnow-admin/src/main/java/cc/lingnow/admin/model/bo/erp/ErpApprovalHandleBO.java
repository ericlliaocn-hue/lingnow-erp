package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ErpApprovalHandleBO {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    private String comment;
    private Long transferUserId;
}
