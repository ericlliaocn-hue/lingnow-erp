package cc.lingnow.admin.model.bo.erp;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ErpBillQueryBO {
    private Long current = 1L;
    private Long size = 10L;
    private String billNo;
    private Long employeeId;
    private Long partnerId;
    private Integer auditStatus;
    private String paymentStatus;
    private LocalDate beginDate;
    private LocalDate endDate;
}
