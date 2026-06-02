package cc.lingnow.biz.erp.entity;

import cc.lingnow.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_bill_no_rule")
public class ErpBillNoRule extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String billType;
    private String billName;
    private String prefix;
    private String datePattern;
    private Integer serialLength;
    private Long nextSerial;
    private String resetCycle;
    private String lastDatePart;
    private Integer enabled;
    private String remark;
}
