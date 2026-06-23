package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ErpBillSaveBO {
    private Long id;
    private String billNo;
    @NotNull(message = "单据日期不能为空")
    private LocalDate billDate;
    @NotNull(message = "往来单位不能为空")
    private Long partnerId;
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;
    private Long accountId;
    private Long employeeId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal discountAmount;
    private BigDecimal otherAmount;
    private BigDecimal paidAmount;
    private String paymentMethod;
    private String remark;
    private String attachmentUrl;
    @Valid
    @NotEmpty(message = "商品明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        @NotNull(message = "商品不能为空")
        private Long productId;
        private Long warehouseId;
        private String attributeText;
        private Long categoryLevel1Id;
        private String categoryLevel1Name;
        private Long categoryLevel2Id;
        private String categoryLevel2Name;
        private String optionAttributeIds;
        private String optionAttributeText;
        @NotNull(message = "数量不能为空")
        private BigDecimal qty;
        @NotNull(message = "单价不能为空")
        private BigDecimal price;
        private BigDecimal discountRate;
        private BigDecimal discountAmount;
        private String remark;
    }
}
