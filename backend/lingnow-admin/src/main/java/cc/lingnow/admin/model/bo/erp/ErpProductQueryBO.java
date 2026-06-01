package cc.lingnow.admin.model.bo.erp;

import lombok.Data;

@Data
public class ErpProductQueryBO {
    private Long current = 1L;
    private Long size = 10L;
    private String code;
    private String name;
    private String barcode;
    private Long categoryId;
    private Long brandId;
    private Integer status;
}
