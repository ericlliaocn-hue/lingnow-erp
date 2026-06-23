package cc.lingnow.biz.erp.entity;

import cc.lingnow.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_product")
public class ErpProduct extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
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
