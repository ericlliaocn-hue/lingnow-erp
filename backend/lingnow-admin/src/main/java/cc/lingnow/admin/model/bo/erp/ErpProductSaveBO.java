package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpProductSaveBO {
    private Long id;
    @NotBlank(message = "商品编号不能为空")
    private String code;
    @NotBlank(message = "商品名称不能为空")
    private String name;
    private String spec;
    private Long categoryId;
    private Long brandId;
    private Long unitId;
    private String attributeIds;
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
}
