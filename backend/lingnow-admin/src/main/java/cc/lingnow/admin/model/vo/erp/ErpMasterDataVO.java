package cc.lingnow.admin.model.vo.erp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "ERP基础资料VO")
public class ErpMasterDataVO {

    private Long id;
    private String code;
    private String name;
    private Long parentId;
    private String contact;
    private String phone;
    private String address;
    private Long levelId;
    private String accountType;
    private BigDecimal openingBalance;
    private BigDecimal discountRate;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
