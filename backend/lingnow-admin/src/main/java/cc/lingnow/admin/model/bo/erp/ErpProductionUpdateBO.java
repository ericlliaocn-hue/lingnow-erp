package cc.lingnow.admin.model.bo.erp;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ErpProductionUpdateBO {

    @Size(max = 64, message = "生产进度不能超过64个字符")
    private String productionProgress;

    @Size(max = 100, message = "快递单号不能超过100个字符")
    private String trackingNo;

    @Size(max = 64, message = "生产人员不能超过64个字符")
    private String productionUserName;

    private Long productionUserId;
}
