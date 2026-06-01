package cc.lingnow.admin.model.vo.erp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpBillVO {
    private Long id;
    private String billNo;
    private String billType;
    private LocalDate billDate;
    private Long partnerId;
    private String partnerName;
    private String partnerType;
    private Long warehouseId;
    private String warehouseName;
    private Long accountId;
    private String accountName;
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
    private LocalDateTime auditTime;
    private String auditBy;
    private String remark;
    private String attachmentUrl;
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
        private BigDecimal qty;
        private BigDecimal price;
        private BigDecimal amount;
        private BigDecimal discountRate;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private String remark;
    }
}
