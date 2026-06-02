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
@TableName("erp_stock_check_item")
public class ErpStockCheckItem extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long checkId;
    private Long productId;
    private String productCode;
    private String productName;
    private String spec;
    private Long unitId;
    private Long warehouseId;
    private BigDecimal bookQty;
    private BigDecimal checkQty;
    private BigDecimal diffQty;
    private BigDecimal costPrice;
    private BigDecimal diffAmount;
    private String remark;
}
