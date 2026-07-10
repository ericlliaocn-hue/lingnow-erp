package cc.lingnow.biz.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_product_attribute")
public class ErpProductAttribute extends ErpMasterData {
    private BigDecimal extraAmount;
}
