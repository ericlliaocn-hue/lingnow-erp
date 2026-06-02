package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ErpPrintTemplateSaveBO {
    private Long id;
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    @NotBlank(message = "单据类型不能为空")
    private String billType;
    private String paperType;
    private String contentJson;
    private Integer isDefault;
    private Integer status;
    private String remark;
}
