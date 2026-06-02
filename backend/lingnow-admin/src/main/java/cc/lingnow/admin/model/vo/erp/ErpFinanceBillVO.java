package cc.lingnow.admin.model.vo.erp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ErpFinanceBillVO {
    private Long id;
    private String billNo;
    private String billType;
    private LocalDate billDate;
    private Long partnerId;
    private String partnerName;
    private String partnerType;
    private Long accountId;
    private String accountName;
    private BigDecimal amount;
    private Integer auditStatus;
    private String approvalStatus;
    private Long approvalInstanceId;
    private String approvalSubmitBy;
    private LocalDateTime approvalSubmitTime;
    private LocalDateTime approvalFinishTime;
    private LocalDateTime auditTime;
    private String auditBy;
    private String remark;
    private LocalDateTime createTime;
}
