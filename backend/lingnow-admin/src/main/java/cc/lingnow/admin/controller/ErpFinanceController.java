package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpApprovalSubmitBO;
import cc.lingnow.admin.model.bo.erp.ErpFinanceBillQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpFinanceBillSaveBO;
import cc.lingnow.admin.model.enums.ErpApprovalStatus;
import cc.lingnow.admin.model.vo.erp.ErpFinanceBillVO;
import cc.lingnow.admin.service.ErpApprovalService;
import cc.lingnow.admin.service.ErpAuditService;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/erp/finance")
@RequiredArgsConstructor
public class ErpFinanceController {

    private final ErpFinanceBillService financeBillService;
    private final ErpCustomerService customerService;
    private final ErpSupplierService supplierService;
    private final ErpAccountService accountService;
    private final ErpFundFlowService fundFlowService;
    private final ErpPartnerFlowService partnerFlowService;
    private final ErpBillNoRuleService billNoRuleService;
    private final ErpDataAuthService dataAuthService;
    private final SysUserService userService;
    private final ErpApprovalService approvalService;
    private final ErpAuditService auditService;

    @GetMapping("/{module:receipt|payment|income|expense}/list")
    public Result<PageResult<ErpFinanceBillVO>> list(@PathVariable String module, ErpFinanceBillQueryBO query) {
        check(module, "list");
        QueryWrapper<ErpFinanceBill> wrapper = wrapper(billType(module), query);
        IPage<ErpFinanceBill> page = financeBillService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords().stream().map(this::toVO).toList()));
    }

    @GetMapping("/{module:receipt|payment|income|expense}/nextNo")
    public Result<String> nextNo(@PathVariable String module) {
        check(module, "add");
        return Result.success(nextBillNo(billType(module)));
    }

    @GetMapping("/{module:receipt|payment|income|expense}/{id}")
    public Result<ErpFinanceBillVO> getInfo(@PathVariable String module, @PathVariable Long id) {
        check(module, "list");
        return Result.success(toVO(requireBill(id, billType(module))));
    }

    @PostMapping("/{module:receipt|payment|income|expense}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP财务单据", businessType = BusinessType.INSERT)
    public Result<Void> add(@PathVariable String module, @Valid @RequestBody ErpFinanceBillSaveBO bo) {
        check(module, "add");
        String type = billType(module);
        ErpFinanceBill bill = buildBill(type, bo);
        bill.setBillNo(StrUtil.isBlank(bo.getBillNo()) ? nextBillNo(type) : bo.getBillNo());
        ensureBillNoUnique(bill.getBillNo(), null);
        financeBillService.save(bill);
        auditService.auditFinanceBill(bill.getId());
        return Result.success();
    }

    @PutMapping("/{module:receipt|payment|income|expense}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP财务单据", businessType = BusinessType.UPDATE)
    public Result<Void> edit(@PathVariable String module, @Valid @RequestBody ErpFinanceBillSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        check(module, "edit");
        String type = billType(module);
        ErpFinanceBill old = requireBill(bo.getId(), type);
        rollbackFinanceBillIfAudited(old);
        ErpFinanceBill bill = buildBill(type, bo);
        bill.setId(old.getId());
        bill.setBillNo(StrUtil.isBlank(bo.getBillNo()) ? old.getBillNo() : bo.getBillNo());
        ensureBillNoUnique(bill.getBillNo(), bill.getId());
        financeBillService.updateById(bill);
        auditService.auditFinanceBill(bill.getId());
        return Result.success();
    }

    @DeleteMapping("/{module:receipt|payment|income|expense}/{ids}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP财务单据", businessType = BusinessType.DELETE)
    public Result<Void> remove(@PathVariable String module, @PathVariable List<Long> ids) {
        check(module, "remove");
        String type = billType(module);
        for (Long id : ids) {
            rollbackFinanceBillIfAudited(requireBill(id, type));
        }
        financeBillService.removeByIds(ids);
        return Result.success();
    }

    @PutMapping("/{module:receipt|payment|income|expense}/audit/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP财务单据审核", businessType = BusinessType.UPDATE)
    public Result<Void> audit(@PathVariable String module, @PathVariable Long id) {
        check(module, "audit");
        ErpApprovalSubmitBO bo = new ErpApprovalSubmitBO();
        bo.setBizType(billType(module));
        bo.setBizId(id);
        approvalService.submit(bo);
        return Result.success();
    }

    @PutMapping("/{module:receipt|payment|income|expense}/unaudit/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP财务单据反审核", businessType = BusinessType.UPDATE)
    public Result<Void> unaudit(@PathVariable String module, @PathVariable Long id) {
        check(module, "unaudit");
        ErpFinanceBill bill = requireBill(id, billType(module));
        auditService.unauditFinanceBill(id);
        bill.setAuditStatus(0);
        bill.setAuditTime(null);
        bill.setAuditBy(null);
        bill.setApprovalStatus(ErpApprovalStatus.NONE);
        bill.setApprovalFinishTime(null);
        financeBillService.updateById(bill);
        return Result.success();
    }

    @GetMapping("/fund-flow/list")
    public Result<PageResult<java.util.Map<String, Object>>> fundFlow(ErpFinanceBillQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:finance:fund-flow");
        QueryWrapper<ErpFundFlow> wrapper = new QueryWrapper<ErpFundFlow>()
                .eq(query.getAccountId() != null, "account_id", query.getAccountId())
                .ge(query.getBeginDate() != null, "operate_time", query.getBeginDate())
                .lt(query.getEndDate() != null, "operate_time", query.getEndDate() == null ? null : query.getEndDate().plusDays(1))
                .orderByDesc("operate_time");
        IPage<ErpFundFlow> page = fundFlowService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::fundFlowRow).toList()));
    }

    @GetMapping("/partner-flow/list")
    public Result<PageResult<java.util.Map<String, Object>>> partnerFlow(ErpFinanceBillQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:finance:partner-flow");
        QueryWrapper<ErpPartnerFlow> wrapper = new QueryWrapper<ErpPartnerFlow>()
                .eq(query.getPartnerId() != null, "partner_id", query.getPartnerId())
                .ge(query.getBeginDate() != null, "operate_time", query.getBeginDate())
                .lt(query.getEndDate() != null, "operate_time", query.getEndDate() == null ? null : query.getEndDate().plusDays(1))
                .orderByDesc("operate_time");
        IPage<ErpPartnerFlow> page = partnerFlowService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::partnerFlowRow).toList()));
    }

    private QueryWrapper<ErpFinanceBill> wrapper(String type, ErpFinanceBillQueryBO query) {
        QueryWrapper<ErpFinanceBill> wrapper = new QueryWrapper<ErpFinanceBill>()
                .eq("bill_type", type)
                .like(StrUtil.isNotBlank(query.getBillNo()), "bill_no", query.getBillNo())
                .eq(query.getPartnerId() != null, "partner_id", query.getPartnerId())
                .eq(query.getAccountId() != null, "account_id", query.getAccountId())
                .eq(query.getAuditStatus() != null, "audit_status", query.getAuditStatus())
                .ge(query.getBeginDate() != null, "bill_date", query.getBeginDate())
                .le(query.getEndDate() != null, "bill_date", query.getEndDate())
                .orderByDesc("bill_date")
                .orderByDesc("create_time");
        applyDataAuth(wrapper, type);
        return wrapper;
    }

    private void applyDataAuth(QueryWrapper<ErpFinanceBill> wrapper, String type) {
        if (isAdminUser()) {
            return;
        }
        if (!"RECEIPT".equals(type)) {
            return;
        }
        List<Long> customerIds = dataAuthService.authorizedIds(currentUserId(), "CUSTOMER");
        if (!customerIds.isEmpty()) {
            wrapper.in("partner_id", customerIds);
        }
    }

    private boolean isAdminUser() {
        Long userId = currentUserId();
        if (userId == null) {
            return false;
        }
        SysUser user = userService.getById(userId);
        return userService.isSuperAdmin(user);
    }

    private Long currentUserId() {
        Object loginId = StpAdminUtil.getLoginIdDefaultNull();
        return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
    }

    private ErpFinanceBill buildBill(String type, ErpFinanceBillSaveBO bo) {
        BigDecimal amount = bo.getAmount() == null ? BigDecimal.ZERO : bo.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "金额必须大于0");
        }
        ErpFinanceBill bill = BeanUtil.copyProperties(bo, ErpFinanceBill.class);
        bill.setBillType(type);
        if ("RECEIPT".equals(type)) {
            requireCustomer(bo.getPartnerId());
            bill.setPartnerType("CUSTOMER");
        } else if ("PAYMENT".equals(type)) {
            requireSupplier(bo.getPartnerId());
            bill.setPartnerType("SUPPLIER");
        } else {
            bill.setPartnerId(0L);
            bill.setPartnerType("NONE");
        }
        bill.setAuditStatus(0);
        bill.setApprovalStatus(ErpApprovalStatus.NONE);
        requireEnabledAccount(bill.getAccountId());
        return bill;
    }

    private void addFundFlow(ErpFinanceBill bill, String direction, String remark) {
        BigDecimal before = accountBalance(bill.getAccountId());
        BigDecimal after = "IN".equals(direction) ? before.add(bill.getAmount()) : before.subtract(bill.getAmount());
        ErpFundFlow flow = new ErpFundFlow();
        flow.setFlowNo(flowNo("FD"));
        flow.setSourceBillId(bill.getId());
        flow.setSourceBillNo(bill.getBillNo());
        flow.setSourceBillType(bill.getBillType());
        flow.setAccountId(bill.getAccountId());
        flow.setDirection(direction);
        flow.setAmount(bill.getAmount());
        flow.setBeforeBalance(before);
        flow.setAfterBalance(after);
        flow.setRemark(remark);
        flow.setOperateTime(LocalDateTime.now());
        fundFlowService.save(flow);
    }

    private void addPartnerFlow(ErpFinanceBill bill, String direction, String remark) {
        if (bill.getPartnerId() == null || "NONE".equals(bill.getPartnerType())) {
            return;
        }
        ErpPartnerFlow flow = new ErpPartnerFlow();
        flow.setSourceBillId(bill.getId());
        flow.setSourceBillNo(bill.getBillNo());
        flow.setSourceBillType(bill.getBillType());
        flow.setPartnerId(bill.getPartnerId());
        flow.setPartnerType(bill.getPartnerType());
        flow.setDirection(direction);
        flow.setAmount(bill.getAmount());
        flow.setRemark(remark);
        flow.setOperateTime(LocalDateTime.now());
        partnerFlowService.save(flow);
    }

    private BigDecimal accountBalance(Long accountId) {
        ErpAccount account = accountService.getById(accountId);
        BigDecimal balance = account == null ? BigDecimal.ZERO : account.getOpeningBalance();
        for (ErpFundFlow flow : fundFlowService.list(new QueryWrapper<ErpFundFlow>().eq("account_id", accountId))) {
            balance = "IN".equals(flow.getDirection()) ? balance.add(flow.getAmount()) : balance.subtract(flow.getAmount());
        }
        return balance;
    }

    private ErpFinanceBillVO toVO(ErpFinanceBill bill) {
        ErpFinanceBillVO vo = BeanUtil.copyProperties(bill, ErpFinanceBillVO.class);
        vo.setPartnerName("CUSTOMER".equals(bill.getPartnerType())
                ? masterName(customerService.getById(bill.getPartnerId()))
                : "SUPPLIER".equals(bill.getPartnerType()) ? masterName(supplierService.getById(bill.getPartnerId())) : null);
        vo.setAccountName(masterName(accountService.getById(bill.getAccountId())));
        return vo;
    }

    private ErpFinanceBill requireBill(Long id, String type) {
        ErpFinanceBill bill = financeBillService.getById(id);
        if (bill == null || !type.equals(bill.getBillType())) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return bill;
    }

    private void ensureUnaudited(ErpFinanceBill bill) {
        if (Integer.valueOf(1).equals(bill.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已审核单据不能修改或删除");
        }
        if (ErpApprovalStatus.PENDING.equals(bill.getApprovalStatus()) || ErpApprovalStatus.APPROVED.equals(bill.getApprovalStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "审批中或已审批单据不能修改或删除");
        }
    }

    private void rollbackFinanceBillIfAudited(ErpFinanceBill bill) {
        if (Integer.valueOf(1).equals(bill.getAuditStatus())) {
            auditService.unauditFinanceBill(bill.getId());
        }
    }

    private void ensureCanAudit(ErpFinanceBill bill) {
        if (Integer.valueOf(1).equals(bill.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据已审核，不能重复审核");
        }
    }

    private void ensureBillNoUnique(String billNo, Long id) {
        ErpFinanceBill exists = financeBillService.getOne(new QueryWrapper<ErpFinanceBill>().eq("bill_no", billNo).last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据编号已存在");
        }
    }

    private String nextBillNo(String type) {
        String ruleNo = billNoRuleService.nextNo(type);
        if (StrUtil.isNotBlank(ruleNo)) {
            return ruleNo;
        }
        String prefix = switch (type) {
            case "RECEIPT" -> "SK";
            case "PAYMENT" -> "FK";
            case "INCOME" -> "QTSR";
            default -> "QTZC";
        };
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String like = prefix + "-" + date + "-";
        long count = financeBillService.count(new QueryWrapper<ErpFinanceBill>().likeRight("bill_no", like));
        return like + String.format("%04d", count + 1);
    }

    private String flowNo(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String billType(String module) {
        return switch (module) {
            case "receipt" -> "RECEIPT";
            case "payment" -> "PAYMENT";
            case "income" -> "INCOME";
            default -> "EXPENSE";
        };
    }

    private void check(String module, String action) {
        StpAdminUtil.stpLogic.checkPermission("erp:finance:" + module + ":" + action);
    }

    private String masterName(ErpMasterData data) {
        return data == null ? null : data.getName();
    }

    private java.util.Map<String, Object> fundFlowRow(ErpFundFlow flow) {
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", flow.getId());
        row.put("flowNo", flow.getFlowNo());
        row.put("operateTime", flow.getOperateTime());
        row.put("sourceBillNo", flow.getSourceBillNo());
        row.put("sourceBillType", flow.getSourceBillType());
        row.put("accountId", flow.getAccountId());
        row.put("accountName", masterName(accountService.getById(flow.getAccountId())));
        row.put("direction", flow.getDirection());
        row.put("amount", flow.getAmount());
        row.put("beforeBalance", flow.getBeforeBalance());
        row.put("afterBalance", flow.getAfterBalance());
        row.put("remark", flow.getRemark());
        return row;
    }

    private java.util.Map<String, Object> partnerFlowRow(ErpPartnerFlow flow) {
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", flow.getId());
        row.put("operateTime", flow.getOperateTime());
        row.put("sourceBillNo", flow.getSourceBillNo());
        row.put("sourceBillType", flow.getSourceBillType());
        row.put("partnerId", flow.getPartnerId());
        row.put("partnerType", flow.getPartnerType());
        row.put("partnerName", "CUSTOMER".equals(flow.getPartnerType())
                ? masterName(customerService.getById(flow.getPartnerId()))
                : "SUPPLIER".equals(flow.getPartnerType()) ? masterName(supplierService.getById(flow.getPartnerId())) : null);
        row.put("direction", flow.getDirection());
        row.put("amount", flow.getAmount());
        row.put("remark", flow.getRemark());
        return row;
    }

    private void requirePartner(Long partnerId) {
        if (partnerId == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "往来单位不能为空");
        }
    }

    private void requireCustomer(Long partnerId) {
        requirePartner(partnerId);
        ErpCustomer customer = customerService.getById(partnerId);
        if (customer == null || !Integer.valueOf(1).equals(customer.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "客户不存在或已停用");
        }
    }

    private void requireSupplier(Long partnerId) {
        requirePartner(partnerId);
        ErpSupplier supplier = supplierService.getById(partnerId);
        if (supplier == null || !Integer.valueOf(1).equals(supplier.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "供应商不存在或已停用");
        }
    }

    private void requireEnabledAccount(Long accountId) {
        ErpAccount account = accountService.getById(accountId);
        if (account == null || !Integer.valueOf(1).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户不存在或已停用");
        }
    }
}
