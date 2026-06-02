package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ErpFieldSettingSaveBO {
    private Long id;
    @NotBlank(message = "模块编码不能为空")
    private String moduleCode;
    @NotBlank(message = "字段键不能为空")
    private String fieldKey;
    @NotBlank(message = "字段名称不能为空")
    private String fieldLabel;
    private Integer visible;
    private Integer required;
    private Integer sortOrder;
    private Integer width;
    private String remark;
}
