package cc.lingnow.admin.model.vo.shop;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShopProductVO {
    private Long id;
    private String code;
    private String name;
    private String spec;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private String attributeIds;
    private String optionAttributeIds;
    private String attributeText;
    private BigDecimal salePrice;
    private String unitName;
}
