package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ErpBillNoRuleSaveBO {
    private Long id;
    @NotBlank(message = "单据类型不能为空")
    private String billType;
    @NotBlank(message = "规则名称不能为空")
    private String billName;
    @NotBlank(message = "前缀不能为空")
    private String prefix;
    private String datePattern;
    @NotNull(message = "流水长度不能为空")
    private Integer serialLength;
    @NotNull(message = "下一流水号不能为空")
    private Long nextSerial;
    private String resetCycle;
    private Integer enabled;
    private String remark;
}
