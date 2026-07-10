package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpCustomerOrderConfirmBO {
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;
    private Long employeeId;
    private String employeeName;
    private Long accountId;
    private BigDecimal paidAmount;
    private String paymentMethod;
    private String remark;
}
