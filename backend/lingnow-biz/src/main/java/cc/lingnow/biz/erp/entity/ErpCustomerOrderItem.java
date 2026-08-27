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
@TableName("erp_customer_order_item")
public class ErpCustomerOrderItem extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Long productId;
    private String productCode;
    private String productName;
    private String productImageUrl;
    private String logoImageUrl;
    private String spec;
    private String attributeText;
    private Long categoryLevel1Id;
    private String categoryLevel1Name;
    private Long categoryLevel2Id;
    private String categoryLevel2Name;
    private String optionAttributeIds;
    private String optionAttributeText;
    private String optionAttributeQuantityJson;
    private Long unitId;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal amount;
    private String remark;
}
