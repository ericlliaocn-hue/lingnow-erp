package cc.lingnow.biz.erp.entity;

import cc.lingnow.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_bill")
public class ErpBill extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String billNo;
    private String billType;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate billDate;
    private Long partnerId;
    private String partnerType;
    private Long warehouseId;
    private Long accountId;
    private Long employeeId;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal otherAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private BigDecimal debtAmount;
    private Integer auditStatus;
    private String paymentStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    private String auditBy;
    private String remark;
    private String attachmentUrl;
}
