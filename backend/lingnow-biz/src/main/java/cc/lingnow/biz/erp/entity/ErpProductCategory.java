package cc.lingnow.biz.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_product_category")
public class ErpProductCategory extends ErpMasterData {
    private String attributeIds;
}
