package cc.lingnow.admin.model.bo.erp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "ERP基础资料保存参数")
public class ErpMasterDataSaveBO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "编码")
    @NotBlank(message = "编码不能为空")
    private String code;

    @Schema(description = "名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "代理等级ID")
    private Long levelId;

    @Schema(description = "账户类型")
    private String accountType;

    @Schema(description = "期初余额")
    private BigDecimal openingBalance;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "关联属性节点ID集合")
    private String attributeIds;

    @Schema(description = "排序")
    private Integer sortOrder = 0;

    @Schema(description = "状态")
    private Integer status = 1;

    @Schema(description = "备注")
    private String remark;
}
