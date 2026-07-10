package cc.lingnow.admin.model.bo.shop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShopOrderSubmitBO {
    @NotBlank(message = "收货人不能为空")
    private String receiverName;
    @NotBlank(message = "联系电话不能为空")
    private String receiverPhone;
    @NotBlank(message = "收货地址不能为空")
    private String receiverAddress;
    private String remark;
    @Valid
    @NotEmpty(message = "商品明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "商品不能为空")
        private Long productId;
        private String optionAttributeIds;
        private String logoImageUrl;
        @NotNull(message = "数量不能为空")
        private BigDecimal qty;
        private String remark;
    }
}
