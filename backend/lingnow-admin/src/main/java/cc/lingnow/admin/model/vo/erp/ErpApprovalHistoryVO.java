package cc.lingnow.admin.model.vo.erp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpApprovalHistoryVO {
    private Long id;
    private Long taskId;
    private Long instanceId;
    private String nodeName;
    private String targetNodeName;
    private String approver;
    private String skipType;
    private String flowStatus;
    private String message;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
