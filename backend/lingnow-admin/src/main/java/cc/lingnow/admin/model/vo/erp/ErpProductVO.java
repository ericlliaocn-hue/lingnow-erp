package cc.lingnow.admin.model.vo.erp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpProductVO {
    private Long id;
    private String code;
    private String name;
    private String spec;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private Long unitId;
    private String unitName;
    private String attributeText;
    private String barcode;
    private String location;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal retailPrice;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private String imageUrl;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
