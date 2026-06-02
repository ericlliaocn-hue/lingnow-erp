package cc.lingnow.admin.model.bo.erp;

import lombok.Data;

@Data
public class ErpApprovalQueryBO {
    private Long current = 1L;
    private Long size = 10L;
    private String bizType;
    private String billNo;
    private String approvalStatus;
}
