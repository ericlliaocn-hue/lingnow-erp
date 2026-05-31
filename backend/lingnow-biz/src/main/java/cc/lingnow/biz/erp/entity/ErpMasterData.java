package cc.lingnow.biz.erp.entity;

import cc.lingnow.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ERP基础资料通用实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpMasterData extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;

    private String name;

    private Long parentId;

    private String contact;

    private String phone;

    private String address;

    private Long levelId;

    private String accountType;

    private BigDecimal openingBalance;

    private BigDecimal discountRate;

    private Integer sortOrder;

    private Integer status;

    private String remark;
}
