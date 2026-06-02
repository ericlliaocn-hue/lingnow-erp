package cc.lingnow.admin.model.vo.erp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpStockCheckVO {
    private Long id;
    private String checkNo;
    private LocalDate checkDate;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal totalProfitQty;
    private BigDecimal totalLossQty;
    private BigDecimal totalProfitAmount;
    private BigDecimal totalLossAmount;
    private Integer auditStatus;
    private String approvalStatus;
    private Long approvalInstanceId;
    private String approvalSubmitBy;
    private LocalDateTime approvalSubmitTime;
    private LocalDateTime approvalFinishTime;
    private LocalDateTime auditTime;
    private String auditBy;
    private String remark;
    private LocalDateTime createTime;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Long productId;
        private String productCode;
        private String productName;
        private String spec;
        private Long unitId;
        private String unitName;
        private Long warehouseId;
        private BigDecimal bookQty;
        private BigDecimal checkQty;
        private BigDecimal diffQty;
        private BigDecimal costPrice;
        private BigDecimal diffAmount;
        private String remark;
    }
}
