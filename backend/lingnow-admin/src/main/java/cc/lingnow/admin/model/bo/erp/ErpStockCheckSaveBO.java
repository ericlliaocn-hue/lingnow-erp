package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ErpStockCheckSaveBO {
    private Long id;
    private String checkNo;
    @NotNull(message = "盘点日期不能为空")
    private LocalDate checkDate;
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;
    private String remark;
    @Valid
    @NotEmpty(message = "盘点明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        @NotNull(message = "商品不能为空")
        private Long productId;
        private BigDecimal checkQty;
        private String remark;
    }
}
