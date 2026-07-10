package cc.lingnow.admin.model.bo.erp;

import lombok.Data;

@Data
public class ErpCustomerAccountQueryBO {
    private Long current = 1L;
    private Long size = 10L;
    private String username;
    private String customerName;
    private Integer status;
}
