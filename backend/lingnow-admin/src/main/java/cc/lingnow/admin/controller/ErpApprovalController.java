package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpApprovalHandleBO;
import cc.lingnow.admin.model.bo.erp.ErpApprovalQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpApprovalSubmitBO;
import cc.lingnow.admin.model.vo.erp.ErpApprovalHistoryVO;
import cc.lingnow.admin.model.vo.erp.ErpApprovalTaskVO;
import cc.lingnow.admin.service.ErpApprovalService;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/erp/approval")
@RequiredArgsConstructor
public class ErpApprovalController {

    private final ErpApprovalService approvalService;

    @PostMapping("/submit")
    @Log(title = "ERP提交审批", businessType = BusinessType.UPDATE)
    public Result<Void> submit(@Valid @RequestBody ErpApprovalSubmitBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:submit");
        approvalService.submit(bo);
        return Result.success();
    }

    @PostMapping("/pass")
    @Log(title = "ERP审批通过", businessType = BusinessType.UPDATE)
    public Result<Void> pass(@Valid @RequestBody ErpApprovalHandleBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:approve");
        approvalService.pass(bo);
        return Result.success();
    }

    @PostMapping("/reject")
    @Log(title = "ERP审批驳回", businessType = BusinessType.UPDATE)
    public Result<Void> reject(@Valid @RequestBody ErpApprovalHandleBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:reject");
        approvalService.reject(bo);
        return Result.success();
    }

    @PostMapping("/revoke")
    @Log(title = "ERP审批撤回", businessType = BusinessType.UPDATE)
    public Result<Void> revoke(@Valid @RequestBody ErpApprovalSubmitBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:revoke");
        approvalService.revoke(bo);
        return Result.success();
    }

    @PostMapping("/transfer")
    @Log(title = "ERP审批转交", businessType = BusinessType.UPDATE)
    public Result<Void> transfer(@Valid @RequestBody ErpApprovalHandleBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:transfer");
        approvalService.transfer(bo);
        return Result.success();
    }

    @GetMapping("/todo/list")
    public Result<PageResult<ErpApprovalTaskVO>> todo(ErpApprovalQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:task");
        return Result.success(approvalService.todo(query));
    }

    @GetMapping("/done/list")
    public Result<PageResult<ErpApprovalTaskVO>> done(ErpApprovalQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:task");
        return Result.success(approvalService.done(query));
    }

    @GetMapping("/mine/list")
    public Result<PageResult<ErpApprovalTaskVO>> mine(ErpApprovalQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:task");
        return Result.success(approvalService.mine(query));
    }

    @GetMapping("/history")
    public Result<List<ErpApprovalHistoryVO>> history(@RequestParam String bizType, @RequestParam Long bizId) {
        StpAdminUtil.stpLogic.checkPermission("erp:approval:task");
        return Result.success(approvalService.history(bizType, bizId));
    }
}
