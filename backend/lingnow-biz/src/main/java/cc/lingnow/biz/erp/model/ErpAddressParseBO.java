package cc.lingnow.biz.erp.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ErpAddressParseBO {

    @NotBlank(message = "识别内容不能为空")
    private String rawText;
}
