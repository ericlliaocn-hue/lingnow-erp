package cc.lingnow.admin.model.bo.erp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "ERP基础资料查询参数")
public class ErpMasterDataQueryBO {

    @Schema(description = "当前页")
    private Long current = 1L;

    @Schema(description = "每页数量")
    private Long size = 10L;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "联系电话")
    private String phone;
}
