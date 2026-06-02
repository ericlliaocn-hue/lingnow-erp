package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ErpFinanceBillSaveBO {
    private Long id;
    private String billNo;
    @NotNull(message = "单据日期不能为空")
    private LocalDate billDate;
    private Long partnerId;
    @NotNull(message = "账户不能为空")
    private Long accountId;
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;
    private String remark;
}
