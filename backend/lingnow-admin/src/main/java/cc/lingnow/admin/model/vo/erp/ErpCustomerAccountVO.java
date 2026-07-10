package cc.lingnow.admin.model.vo.erp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpCustomerAccountVO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String username;
    private String nickname;
    private String phone;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String remark;
    private LocalDateTime createTime;
}
