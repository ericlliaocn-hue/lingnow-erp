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
    private String employeeName;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;
    private BigDecimal costAmount;
    private BigDecimal discountAmount;
    private BigDecimal otherAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private Boolean sample;
    private BigDecimal profitCostAmount;
    private String paymentMethod;
    private BigDecimal debtAmount;
    private Integer auditStatus;
    private String paymentStatus;
    private String productionProgress;
    private String trackingNo;
    private Long productionUserId;
    private String productionUserName;
    private String approvalStatus;
    private Long approvalInstanceId;
    private String approvalSubmitBy;
    private LocalDateTime approvalSubmitTime;
    private LocalDateTime approvalFinishTime;
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
        private String unitName;
        private Long warehouseId;
        private BigDecimal qty;
        private BigDecimal basePrice;
        private BigDecimal attributeExtraAmount;
        private BigDecimal costPrice;
        private BigDecimal price;
        private BigDecimal amount;
        private BigDecimal discountRate;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private String remark;
    }
}
