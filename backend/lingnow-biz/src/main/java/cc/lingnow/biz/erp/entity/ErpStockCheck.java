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
@TableName("erp_stock_check")
public class ErpStockCheck extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String checkNo;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkDate;
    private Long warehouseId;
    private BigDecimal totalProfitQty;
    private BigDecimal totalLossQty;
    private BigDecimal totalProfitAmount;
    private BigDecimal totalLossAmount;
    private Integer auditStatus;
    private String approvalStatus;
    private Long approvalInstanceId;
    private String approvalSubmitBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalSubmitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalFinishTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    private String auditBy;
    private String remark;
}
