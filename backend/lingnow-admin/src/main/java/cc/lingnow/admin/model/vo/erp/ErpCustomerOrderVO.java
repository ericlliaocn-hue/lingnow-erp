package cc.lingnow.admin.model.vo.erp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpCustomerOrderVO {
    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private Long accountId;
    private String accountName;
    private Long employeeId;
    private String employeeName;
    private String source;
    private String status;
    private LocalDateTime orderTime;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private Long billId;
    private String billNo;
    private LocalDateTime confirmTime;
    private String confirmBy;
    private LocalDateTime cancelTime;
    private String cancelBy;
    private String cancelReason;
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
        private String optionAttributeIds;
        private String optionAttributeText;
        private BigDecimal qty;
        private BigDecimal price;
        private BigDecimal amount;
        private String remark;
    }
}
