package cc.lingnow.admin.model.bo.erp;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ErpFinanceBillQueryBO {
    private Long current = 1L;
    private Long size = 10L;
    private String billNo;
    private Long partnerId;
    private Long accountId;
    private Integer auditStatus;
    private LocalDate beginDate;
    private LocalDate endDate;
}
