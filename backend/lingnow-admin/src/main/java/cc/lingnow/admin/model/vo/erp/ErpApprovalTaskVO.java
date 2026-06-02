package cc.lingnow.admin.model.vo.erp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpApprovalTaskVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long instanceId;
    private String bizType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizId;
    private String bizName;
    private String billNo;
    private String approvalStatus;
    private String nodeCode;
    private String nodeName;
    private String flowStatus;
    private String amount;
    private String submitBy;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
    private String actionUrl;
}
