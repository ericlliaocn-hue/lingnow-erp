package cc.lingnow.admin.service;

import cc.lingnow.admin.model.bo.erp.ErpApprovalHandleBO;
import cc.lingnow.admin.model.bo.erp.ErpApprovalQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpApprovalSubmitBO;
import cc.lingnow.admin.model.vo.erp.ErpApprovalHistoryVO;
import cc.lingnow.admin.model.vo.erp.ErpApprovalTaskVO;
import cc.lingnow.common.vo.PageResult;

import java.util.List;

public interface ErpApprovalService {

    void submit(ErpApprovalSubmitBO bo);

    void pass(ErpApprovalHandleBO bo);

    void reject(ErpApprovalHandleBO bo);

    void revoke(ErpApprovalSubmitBO bo);

    void transfer(ErpApprovalHandleBO bo);

    PageResult<ErpApprovalTaskVO> todo(ErpApprovalQueryBO query);

    PageResult<ErpApprovalTaskVO> done(ErpApprovalQueryBO query);

    PageResult<ErpApprovalTaskVO> mine(ErpApprovalQueryBO query);

    List<ErpApprovalHistoryVO> history(String bizType, Long bizId);
}
