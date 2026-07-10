package cc.lingnow.admin.model.bo.erp;

import lombok.Data;

@Data
public class ErpCustomerOrderQueryBO {
    private Long current = 1L;
    private Long size = 10L;
    private String orderNo;
    private String customerName;
    private String status;
}
