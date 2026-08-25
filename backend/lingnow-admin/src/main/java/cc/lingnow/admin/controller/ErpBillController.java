package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpBillQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpBillSaveBO;
import cc.lingnow.admin.model.bo.erp.ErpProductionUpdateBO;
import cc.lingnow.admin.model.bo.erp.ErpApprovalSubmitBO;
import cc.lingnow.admin.model.enums.ErpApprovalStatus;
import cc.lingnow.admin.model.vo.erp.ErpBillVO;
import cc.lingnow.admin.service.ErpApprovalService;
import cc.lingnow.admin.service.ErpAuditService;
import cc.lingnow.admin.util.CsvExportUtil;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.biz.notification.service.SysUserNotificationService;
import cc.lingnow.biz.role.service.SysRoleService;
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
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "ERP销售进货单")
@RestController
@RequestMapping("/erp")
@RequiredArgsConstructor
public class ErpBillController {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final Set<String> PAYMENT_METHODS = Set.of("淘宝", "1688", "小红书", "微信", "支付宝", "银行卡");

    private final ErpBillService billService;
    private final ErpBillItemService billItemService;
    private final ErpProductService productService;
    private final ErpCustomerService customerService;
    private final ErpSupplierService supplierService;
    private final ErpWarehouseService warehouseService;
    private final ErpAccountService accountService;
    private final ErpUnitService unitService;
    private final ErpProductAttributeService attributeService;
    private final ErpStockBalanceService stockBalanceService;
    private final ErpStockFlowService stockFlowService;
    private final ErpFundFlowService fundFlowService;
    private final ErpPartnerFlowService partnerFlowService;
    private final ErpBillNoRuleService billNoRuleService;
    private final ErpDataAuthService dataAuthService;
    private final ErpApprovalService approvalService;
    private final ErpAuditService auditService;
    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysUserNotificationService notificationService;

    @GetMapping("/{module:sale|sale-return|purchase|purchase-return|production}/list")
    public Result<PageResult<ErpBillVO>> list(@PathVariable String module, ErpBillQueryBO query) {
        String type = billType(module);
        check(module, "list");
        QueryWrapper<ErpBill> wrapper = billListWrapper(type, query, "production".equals(module));
        IPage<ErpBill> page = billService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        boolean includeCost = "production".equals(module);
        List<ErpBillVO> records = page.getRecords().stream().map(item -> toVO(item, false, includeCost)).toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @GetMapping("/{module:sale|sale-return|purchase|purchase-return}/nextNo")
    public Result<String> nextNo(@PathVariable String module) {
        check(module, "add");
        return Result.success(nextBillNo(billType(module)));
    }

    @GetMapping("/{module:sale|sale-return|purchase|purchase-return|production}/{id}")
    public Result<ErpBillVO> getInfo(@PathVariable String module, @PathVariable Long id) {
        check(module, "list");
        ErpBill bill = requireBill(id, billType(module));
        if ("production".equals(module)) {
            checkProductionVisible(bill);
        }
        return Result.success(toVO(bill, true, "production".equals(module)));
    }

    @GetMapping("/{module:sale|sale-return|purchase|purchase-return}/export")
    @Log(title = "ERP业务单据", businessType = BusinessType.EXPORT, isSaveResponseData = false)
    public void export(@PathVariable String module, ErpBillQueryBO query, HttpServletResponse response) throws Exception {
        String type = billType(module);
        check(module, "export");
        List<List<String>> rows = billService.list(billListWrapper(type, query)).stream().map(bill -> {
            ErpBillVO vo = toVO(bill, false);
            return List.of(
                    text(vo.getBillNo()),
                    text(vo.getBillDate()),
                    text(vo.getPartnerName()),
                    text(vo.getWarehouseName()),
                    money(vo.getTotalAmount()),
                    money(vo.getDiscountAmount()),
                    money(vo.getOtherAmount()),
                    money(vo.getPayableAmount()),
                    text(vo.getPaymentMethod()),
                    money(vo.getPaidAmount()),
                    text(vo.getPaymentStatus()),
                    text(vo.getRemark())
            );
        }).toList();
        CsvExportUtil.write(response, titleName(module) + ".csv",
                List.of("单号", "日期", partnerTitle(type), "仓库", "合计金额", "优惠金额", "其他费用", "应收应付", "付款方式", "付款金额", "收付状态", "备注"),
                rows);
    }

    @GetMapping("/{module:sale|sale-return|purchase|purchase-return|production}/print/{id}")
    @Log(title = "ERP业务单据", businessType = BusinessType.OTHER)
    public Result<Map<String, Object>> printPreview(@PathVariable String module, @PathVariable Long id) {
        check(module, "print");
        ErpBill source = requireBill(id, billType(module));
        if ("production".equals(module)) {
            checkProductionVisible(source);
        }
        ErpBillVO bill = toVO(source, true);
        Map<String, Object> data = new HashMap<>();
        data.put("title", titleName(module));
        data.put("bill", bill);
        data.put("items", bill.getItems());
        data.put("fields", printFields(module, bill.getBillType()));
        data.put("itemFields", List.of("商品编号", "商品名称", "商品图片", "LOGO图片", "规格", "类目选项", "单位", "仓库", "数量", "单价", "金额", "优惠", "折后金额", "备注"));
        return Result.success(data);
    }

    @PostMapping("/{module:sale|sale-return|purchase|purchase-return}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP业务单据", businessType = BusinessType.INSERT)
    public Result<Void> add(@PathVariable String module, @Valid @RequestBody ErpBillSaveBO bo) {
        check(module, "add");
        String type = billType(module);
        ErpBill bill = buildBill(type, bo);
        if (bill.getEmployeeId() == null && type.startsWith("SALE")) {
            bill.setEmployeeId(currentUserId());
        }
        normalizeSaleEmployeeName(bill, null);
        bill.setBillNo(StrUtil.isBlank(bo.getBillNo()) ? nextBillNo(type) : bo.getBillNo());
        ensureBillNoUnique(bill.getBillNo(), null);
        billService.save(bill);
        saveItems(bill, bo.getItems());
        auditService.auditBill(bill.getId());
        notifyNewSaleBill(bill);
        return Result.success();
    }

    @PutMapping("/{module:sale|sale-return|purchase|purchase-return}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP业务单据", businessType = BusinessType.UPDATE)
    public Result<Void> edit(@PathVariable String module, @Valid @RequestBody ErpBillSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        check(module, "edit");
        String type = billType(module);
        ErpBill old = requireBill(bo.getId(), type);
        rollbackBillIfAudited(old);
        ErpBill bill = buildBill(type, bo);
        bill.setId(old.getId());
        bill.setProfitCostAmount(old.getProfitCostAmount());
        if (bill.getEmployeeId() == null) {
            bill.setEmployeeId(old.getEmployeeId());
        }
        normalizeSaleEmployeeName(bill, old);
        bill.setBillNo(StrUtil.isBlank(bo.getBillNo()) ? old.getBillNo() : bo.getBillNo());
        ensureBillNoUnique(bill.getBillNo(), bill.getId());
        billService.updateById(bill);
        billItemService.remove(new QueryWrapper<ErpBillItem>().eq("bill_id", bill.getId()));
        saveItems(bill, bo.getItems());
        auditService.auditBill(bill.getId());
        return Result.success();
    }

    @DeleteMapping("/{module:sale|sale-return|purchase|purchase-return}/{ids}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP业务单据", businessType = BusinessType.DELETE)
    public Result<Void> remove(@PathVariable String module, @PathVariable List<Long> ids) {
        check(module, "remove");
        String type = billType(module);
        for (Long id : ids) {
            ErpBill bill = requireBill(id, type);
            rollbackBillIfAudited(bill);
            billItemService.remove(new QueryWrapper<ErpBillItem>().eq("bill_id", id));
        }
        billService.removeByIds(ids);
        return Result.success();
    }

    @PutMapping("/production/{id}")
    @Log(title = "生产单维护", businessType = BusinessType.UPDATE)
    public Result<Void> updateProduction(@PathVariable Long id, @Valid @RequestBody ErpProductionUpdateBO bo) {
        check("production", "edit");
        checkProductionAdmin();
        requireBill(id, "SALE");
        String productionUserName = blankToNull(bo.getProductionUserName());
        if (productionUserName == null && bo.getProductionUserId() != null) {
            productionUserName = userDisplayName(bo.getProductionUserId());
        }
        billService.update(new UpdateWrapper<ErpBill>()
                .eq("id", id)
                .set("production_progress", blankToNull(bo.getProductionProgress()))
                .set("tracking_no", blankToNull(bo.getTrackingNo()))
                .set("production_user_id", null)
                .set("production_user_name", productionUserName));
        return Result.success();
    }

    @PostMapping("/{module:sale|sale-return|purchase|purchase-return|production}/copy/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP业务单据", businessType = BusinessType.INSERT)
    public Result<Long> copy(@PathVariable String module, @PathVariable Long id) {
        check(module, "production".equals(module) ? "copy" : "add");
        String type = billType(module);
        ErpBill source = requireBill(id, type);
        if ("production".equals(module)) {
            checkProductionVisible(source);
        }
        ErpBill copy = BeanUtil.copyProperties(source, ErpBill.class);
        copy.setId(null);
        copy.setBillNo(nextBillNo(type));
        copy.setProductionProgress(null);
        copy.setTrackingNo(null);
        copy.setProductionUserId(null);
        copy.setProductionUserName(null);
        copy.setAuditStatus(0);
        copy.setAuditTime(null);
        copy.setAuditBy(null);
        copy.setApprovalStatus(ErpApprovalStatus.NONE);
        copy.setApprovalInstanceId(null);
        copy.setApprovalSubmitBy(null);
        copy.setApprovalSubmitTime(null);
        copy.setApprovalFinishTime(null);
        copy.setProfitCostAmount(null);
        billService.save(copy);

        List<ErpBillItem> sourceItems = billItemService.list(new QueryWrapper<ErpBillItem>().eq("bill_id", source.getId()));
        List<ErpBillItem> copyItems = sourceItems.stream().map(item -> {
            ErpBillItem copyItem = BeanUtil.copyProperties(item, ErpBillItem.class);
            copyItem.setId(null);
            copyItem.setBillId(copy.getId());
            return copyItem;
        }).toList();
        if (!copyItems.isEmpty()) {
            billItemService.saveBatch(copyItems);
        }
        auditService.auditBill(copy.getId());
        return Result.success(copy.getId());
    }

    @PutMapping("/{module:sale|sale-return|purchase|purchase-return}/audit/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP业务单据审核", businessType = BusinessType.UPDATE)
    public Result<Void> audit(@PathVariable String module, @PathVariable Long id) {
        check(module, "audit");
        ErpApprovalSubmitBO bo = new ErpApprovalSubmitBO();
        bo.setBizType(billType(module));
        bo.setBizId(id);
        approvalService.submit(bo);
        return Result.success();
    }

    @PutMapping("/{module:sale|sale-return|purchase|purchase-return}/unaudit/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP业务单据反审核", businessType = BusinessType.UPDATE)
    public Result<Void> unaudit(@PathVariable String module, @PathVariable Long id) {
        check(module, "unaudit");
        ErpBill bill = requireBill(id, billType(module));
        auditService.unauditBill(id);
        bill.setAuditStatus(0);
        bill.setAuditTime(null);
        bill.setAuditBy(null);
        bill.setApprovalStatus(ErpApprovalStatus.NONE);
        bill.setApprovalFinishTime(null);
        billService.updateById(bill);
        return Result.success();
    }

    private void auditPurchase(ErpBill bill, List<ErpBillItem> items) {
        for (ErpBillItem item : items) {
            changeStock(bill, item, "IN");
        }
        addPartnerFlow(bill, "PAYABLE", bill.getPayableAmount(), "进货应付");
        if (positive(bill.getPaidAmount())) {
            addFundFlow(bill.getId(), bill.getBillNo(), bill.getBillType(), bill.getAccountId(), "OUT", bill.getPaidAmount(), "进货付款");
            addPartnerFlow(bill, "PAY", bill.getPaidAmount(), "进货付款");
        }
    }

    private void auditPurchaseReturn(ErpBill bill, List<ErpBillItem> items) {
        for (ErpBillItem item : items) {
            changeStock(bill, item, "OUT");
        }
        addPartnerFlow(bill, "PAY", bill.getPayableAmount(), "进货退货冲应付");
        if (positive(bill.getPaidAmount())) {
            addFundFlow(bill.getId(), bill.getBillNo(), bill.getBillType(), bill.getAccountId(), "IN", bill.getPaidAmount(), "进货退货退款");
            addPartnerFlow(bill, "PAYABLE", bill.getPaidAmount(), "进货退货退款冲往来");
        }
    }

    private void auditSale(ErpBill bill, List<ErpBillItem> items) {
        for (ErpBillItem item : items) {
            changeStock(bill, item, "OUT");
        }
        addPartnerFlow(bill, "RECEIVABLE", bill.getPayableAmount(), "销售应收");
        if (positive(bill.getPaidAmount())) {
            addFundFlow(bill.getId(), bill.getBillNo(), bill.getBillType(), bill.getAccountId(), "IN", bill.getPaidAmount(), "销售收款");
            addPartnerFlow(bill, "RECEIVE", bill.getPaidAmount(), "销售收款");
        }
    }

    private void auditSaleReturn(ErpBill bill, List<ErpBillItem> items) {
        for (ErpBillItem item : items) {
            changeStock(bill, item, "IN");
        }
        addPartnerFlow(bill, "RECEIVE", bill.getPayableAmount(), "销售退货冲应收");
        if (positive(bill.getPaidAmount())) {
            addFundFlow(bill.getId(), bill.getBillNo(), bill.getBillType(), bill.getAccountId(), "OUT", bill.getPaidAmount(), "销售退货退款");
            addPartnerFlow(bill, "RECEIVABLE", bill.getPaidAmount(), "销售退货退款冲往来");
        }
    }

    private QueryWrapper<ErpBill> billListWrapper(String type, ErpBillQueryBO query) {
        return billListWrapper(type, query, false);
    }

    private QueryWrapper<ErpBill> billListWrapper(String type, ErpBillQueryBO query, boolean productionModule) {
        QueryWrapper<ErpBill> wrapper = new QueryWrapper<ErpBill>()
                .eq("bill_type", type)
                .like(StrUtil.isNotBlank(query.getBillNo()), "bill_no", query.getBillNo())
                .eq(query.getPartnerId() != null, "partner_id", query.getPartnerId())
                .eq(query.getAuditStatus() != null, "audit_status", query.getAuditStatus())
                .eq(StrUtil.isNotBlank(query.getPaymentStatus()), "payment_status", query.getPaymentStatus())
                .ge(query.getBeginDate() != null, "bill_date", query.getBeginDate())
                .le(query.getEndDate() != null, "bill_date", query.getEndDate())
                .orderByDesc("bill_date")
                .orderByDesc("create_time");
        if (productionModule) {
            applyProductionDataAuth(wrapper, query);
        } else {
            applyDataAuth(wrapper, type);
        }
        return wrapper;
    }

    private void applyProductionDataAuth(QueryWrapper<ErpBill> wrapper, ErpBillQueryBO query) {
        if (isProductionFullAccessUser()) {
            wrapper.eq(query.getEmployeeId() != null, "employee_id", query.getEmployeeId());
            return;
        }
        Long userId = currentUserId();
        wrapper.eq("employee_id", userId == null ? -1L : userId);
    }

    private void applyDataAuth(QueryWrapper<ErpBill> wrapper, String type) {
        if (isAdminUser()) {
            return;
        }
        Long userId = currentUserId();
        applyIdAuth(wrapper, "warehouse_id", dataAuthService.authorizedIds(userId, "WAREHOUSE"));
        if ("SALE".equals(type) || "SALE_RETURN".equals(type)) {
            applyIdAuth(wrapper, "partner_id", dataAuthService.authorizedIds(userId, "CUSTOMER"));
        }
    }

    private void applyIdAuth(QueryWrapper<ErpBill> wrapper, String column, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        wrapper.in(column, ids);
    }

    private boolean isAdminUser() {
        Long userId = currentUserId();
        if (userId == null) {
            return false;
        }
        SysUser user = userService.getById(userId);
        return userService.isSuperAdmin(user);
    }

    private boolean isProductionFullAccessUser() {
        List<String> permissions = StpAdminUtil.stpLogic.getPermissionList();
        if (permissions.contains("*:*:*")) {
            return true;
        }
        Long userId = currentUserId();
        return userId != null && roleService.selectRoleKeysByUserId(userId).contains("admin");
    }

    private void checkProductionVisible(ErpBill bill) {
        if (isProductionFullAccessUser()) {
            return;
        }
        Long userId = currentUserId();
        if (userId == null || bill.getEmployeeId() == null || !bill.getEmployeeId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只能查看自己录入的生产单");
        }
    }

    private Long currentUserId() {
        Object loginId = StpAdminUtil.getLoginIdDefaultNull();
        return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
    }

    private void changeStock(ErpBill bill, ErpBillItem item, String direction) {
        ErpStockBalance balance = stockBalanceService.getOne(new QueryWrapper<ErpStockBalance>()
                .eq("product_id", item.getProductId()).eq("warehouse_id", item.getWarehouseId()).last("limit 1"));
        if (balance == null) {
            balance = new ErpStockBalance();
            balance.setProductId(item.getProductId());
            balance.setWarehouseId(item.getWarehouseId());
            balance.setQty(BigDecimal.ZERO);
            balance.setCostAmount(BigDecimal.ZERO);
            balance.setAvgCost(BigDecimal.ZERO);
        }
        BigDecimal before = nvl(balance.getQty());
        BigDecimal beforeCost = nvl(balance.getCostAmount());
        BigDecimal beforeAvgCost = nvl(balance.getAvgCost());
        BigDecimal after = "IN".equals(direction) ? before.add(item.getQty()) : before.subtract(item.getQty());
        BigDecimal costPrice = "IN".equals(direction) ? item.getPrice() : beforeAvgCost;
        BigDecimal costAmount = item.getQty().multiply(costPrice).setScale(4, RoundingMode.HALF_UP);
        BigDecimal afterCost = "IN".equals(direction) ? beforeCost.add(costAmount) : beforeCost.subtract(costAmount);
        if (after.compareTo(BigDecimal.ZERO) == 0) {
            afterCost = BigDecimal.ZERO;
        }
        balance.setQty(after);
        balance.setCostAmount(afterCost);
        balance.setAvgCost(after.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : afterCost.divide(after, 4, RoundingMode.HALF_UP));
        if (balance.getId() == null) {
            stockBalanceService.save(balance);
        } else {
            stockBalanceService.updateById(balance);
        }
        ErpStockFlow flow = new ErpStockFlow();
        flow.setFlowNo(flowNo("ST"));
        flow.setSourceBillId(bill.getId());
        flow.setSourceBillNo(bill.getBillNo());
        flow.setSourceBillType(bill.getBillType());
        flow.setProductId(item.getProductId());
        flow.setWarehouseId(item.getWarehouseId());
        flow.setDirection(direction);
        flow.setQty(item.getQty());
        flow.setPrice(costPrice);
        flow.setAmount(costAmount);
        flow.setBeforeQty(before);
        flow.setAfterQty(after);
        flow.setOperateTime(LocalDateTime.now());
        stockFlowService.save(flow);
    }

    private void rollbackStock(ErpBill bill) {
        List<ErpStockFlow> flows = stockFlowService.list(new QueryWrapper<ErpStockFlow>()
                .eq("source_bill_id", bill.getId()).eq("source_bill_type", bill.getBillType()).orderByDesc("create_time"));
        for (ErpStockFlow flow : flows) {
            ErpStockBalance balance = stockBalanceService.getOne(new QueryWrapper<ErpStockBalance>()
                    .eq("product_id", flow.getProductId()).eq("warehouse_id", flow.getWarehouseId()).last("limit 1"));
            if (balance == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "库存余额不存在，无法反审核");
            }
            BigDecimal after = "IN".equals(flow.getDirection())
                    ? balance.getQty().subtract(flow.getQty())
                    : balance.getQty().add(flow.getQty());
            BigDecimal afterCost = "IN".equals(flow.getDirection())
                    ? nvl(balance.getCostAmount()).subtract(nvl(flow.getAmount()))
                    : nvl(balance.getCostAmount()).add(nvl(flow.getAmount()));
            if (after.compareTo(BigDecimal.ZERO) == 0) {
                afterCost = BigDecimal.ZERO;
            }
            balance.setQty(after);
            balance.setCostAmount(afterCost);
            balance.setAvgCost(after.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : afterCost.divide(after, 4, RoundingMode.HALF_UP));
            stockBalanceService.updateById(balance);
        }
        stockFlowService.remove(new QueryWrapper<ErpStockFlow>().eq("source_bill_id", bill.getId()).eq("source_bill_type", bill.getBillType()));
    }

    private void addFundFlow(Long sourceId, String sourceNo, String sourceType, Long accountId, String direction, BigDecimal amount, String remark) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "存在收付款金额时必须选择账户");
        }
        BigDecimal before = accountBalance(accountId);
        BigDecimal after = "IN".equals(direction) ? before.add(amount) : before.subtract(amount);
        ErpFundFlow flow = new ErpFundFlow();
        flow.setFlowNo(flowNo("FD"));
        flow.setSourceBillId(sourceId);
        flow.setSourceBillNo(sourceNo);
        flow.setSourceBillType(sourceType);
        flow.setAccountId(accountId);
        flow.setDirection(direction);
        flow.setAmount(amount);
        flow.setBeforeBalance(before);
        flow.setAfterBalance(after);
        flow.setRemark(remark);
        flow.setOperateTime(LocalDateTime.now());
        fundFlowService.save(flow);
    }

    private void addPartnerFlow(ErpBill bill, String direction, BigDecimal amount, String remark) {
        if (!positive(amount)) {
            return;
        }
        ErpPartnerFlow flow = new ErpPartnerFlow();
        flow.setSourceBillId(bill.getId());
        flow.setSourceBillNo(bill.getBillNo());
        flow.setSourceBillType(bill.getBillType());
        flow.setPartnerId(bill.getPartnerId());
        flow.setPartnerType(bill.getPartnerType());
        flow.setDirection(direction);
        flow.setAmount(amount);
        flow.setRemark(remark);
        flow.setOperateTime(LocalDateTime.now());
        partnerFlowService.save(flow);
    }

    private BigDecimal accountBalance(Long accountId) {
        List<ErpFundFlow> flows = fundFlowService.list(new QueryWrapper<ErpFundFlow>().eq("account_id", accountId));
        BigDecimal balance = nvl(accountService.getById(accountId) == null ? BigDecimal.ZERO : accountService.getById(accountId).getOpeningBalance());
        for (ErpFundFlow flow : flows) {
            balance = "IN".equals(flow.getDirection()) ? balance.add(flow.getAmount()) : balance.subtract(flow.getAmount());
        }
        return balance;
    }

    private ErpBill buildBill(String type, ErpBillSaveBO bo) {
        List<ErpBillItem> items = buildItems(type, null, bo.getWarehouseId(), bo.getItems());
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal itemDiscount = BigDecimal.ZERO;
        BigDecimal finalAmount = BigDecimal.ZERO;
        for (ErpBillItem item : items) {
            totalQty = totalQty.add(item.getQty());
            totalAmount = totalAmount.add(item.getAmount());
            itemDiscount = itemDiscount.add(item.getDiscountAmount());
            finalAmount = finalAmount.add(item.getFinalAmount());
        }
        BigDecimal discountAmount = nvl(bo.getDiscountAmount());
        BigDecimal otherAmount = nvl(bo.getOtherAmount());
        BigDecimal paidAmount = nvl(bo.getPaidAmount());
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0 || otherAmount.compareTo(BigDecimal.ZERO) < 0 || paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠、其他费用和付款金额不能小于0");
        }
        BigDecimal payable = finalAmount.subtract(discountAmount).add(otherAmount);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "应收应付金额不能小于0");
        }
        validatePaymentMethod(bo.getPaymentMethod(), paidAmount);
        ErpBill bill = BeanUtil.copyProperties(bo, ErpBill.class);
        bill.setBillType(type);
        bill.setPartnerType(type.startsWith("SALE") ? "CUSTOMER" : "SUPPLIER");
        bill.setTotalQty(totalQty);
        bill.setTotalAmount(totalAmount);
        bill.setDiscountAmount(discountAmount.add(itemDiscount));
        bill.setOtherAmount(otherAmount);
        bill.setPayableAmount(payable);
        bill.setPaidAmount(paidAmount);
        bill.setSample(type.equals("SALE") && Boolean.TRUE.equals(bo.getSample()));
        bill.setDebtAmount(payable.subtract(paidAmount));
        bill.setPaymentStatus(paymentStatus(payable, paidAmount));
        bill.setAuditStatus(0);
        bill.setApprovalStatus(ErpApprovalStatus.NONE);
        validateBillReferences(bill);
        return bill;
    }

    private void saveItems(ErpBill bill, List<ErpBillSaveBO.Item> sourceItems) {
        List<ErpBillItem> items = buildItems(bill.getBillType(), bill.getId(), bill.getWarehouseId(), sourceItems);
        billItemService.saveBatch(items);
    }

    private List<ErpBillItem> buildItems(String billType, Long billId, Long defaultWarehouseId, List<ErpBillSaveBO.Item> sourceItems) {
        List<ErpBillItem> items = new ArrayList<>();
        for (ErpBillSaveBO.Item source : sourceItems) {
            ErpProduct product = productService.getById(source.getProductId());
            if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品不存在或已停用");
            }
            BigDecimal qty = nvl(source.getQty());
            BigDecimal price = nvl(source.getPrice());
            BigDecimal attributeExtraAmount = billType != null && billType.startsWith("SALE")
                    ? optionExtraAmount(source.getOptionAttributeIds())
                    : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "数量必须大于0");
            }
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单价不能小于0");
            }
            BigDecimal amount = qty.multiply(price).setScale(4, RoundingMode.HALF_UP);
            BigDecimal rate = source.getDiscountRate() == null ? HUNDRED : source.getDiscountRate();
            BigDecimal discountByRate = amount.subtract(amount.multiply(rate).divide(HUNDRED, 4, RoundingMode.HALF_UP));
            BigDecimal discountAmount = source.getDiscountAmount() == null ? discountByRate : source.getDiscountAmount();
            BigDecimal finalAmount = amount.subtract(discountAmount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "明细折后金额不能小于0");
            }
            ErpBillItem item = new ErpBillItem();
            item.setBillId(billId);
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setProductImageUrl(product.getImageUrl());
            item.setLogoImageUrl(source.getLogoImageUrl());
            item.setSpec(product.getSpec());
            item.setAttributeText(StrUtil.isNotBlank(source.getAttributeText()) ? source.getAttributeText() : product.getAttributeText());
            item.setCategoryLevel1Id(source.getCategoryLevel1Id());
            item.setCategoryLevel1Name(source.getCategoryLevel1Name());
            item.setCategoryLevel2Id(source.getCategoryLevel2Id());
            item.setCategoryLevel2Name(source.getCategoryLevel2Name());
            item.setOptionAttributeIds(source.getOptionAttributeIds());
            item.setOptionAttributeText(source.getOptionAttributeText());
            item.setUnitId(product.getUnitId());
            item.setWarehouseId(source.getWarehouseId() == null ? defaultWarehouseId : source.getWarehouseId());
            item.setQty(qty);
            item.setBasePrice(price.subtract(attributeExtraAmount));
            item.setAttributeExtraAmount(attributeExtraAmount);
            item.setCostPrice(nvl(product.getPurchasePrice()));
            item.setPrice(price);
            item.setAmount(amount);
            item.setDiscountRate(rate);
            item.setDiscountAmount(discountAmount);
            item.setFinalAmount(finalAmount);
            item.setRemark(source.getRemark());
            requireEnabledWarehouse(item.getWarehouseId());
            items.add(item);
        }
        return items;
    }

    private ErpBillVO toVO(ErpBill bill, boolean withItems) {
        return toVO(bill, withItems, false);
    }

    private ErpBillVO toVO(ErpBill bill, boolean withItems, boolean includeCost) {
        ErpBillVO vo = BeanUtil.copyProperties(bill, ErpBillVO.class);
        vo.setPartnerName("CUSTOMER".equals(bill.getPartnerType())
                ? masterName(customerService.getById(bill.getPartnerId()))
                : masterName(supplierService.getById(bill.getPartnerId())));
        vo.setEmployeeName(StrUtil.isNotBlank(bill.getEmployeeName()) ? bill.getEmployeeName() : userDisplayName(bill.getEmployeeId()));
        vo.setProductionUserName(StrUtil.isNotBlank(bill.getProductionUserName()) ? bill.getProductionUserName() : userDisplayName(bill.getProductionUserId()));
        vo.setWarehouseName(masterName(warehouseService.getById(bill.getWarehouseId())));
        vo.setAccountName(masterName(accountService.getById(bill.getAccountId())));
        List<ErpBillItem> items = (withItems || includeCost)
                ? billItemService.list(new QueryWrapper<ErpBillItem>().eq("bill_id", bill.getId()))
                : List.of();
        if (includeCost && "SALE".equals(bill.getBillType())) {
            vo.setCostAmount(Boolean.TRUE.equals(bill.getSample())
                    ? BigDecimal.ZERO
                    : bill.getProfitCostAmount() == null ? billCostAmount(items) : bill.getProfitCostAmount());
        }
        if (withItems) {
            vo.setItems(items.stream().map(this::itemVO).toList());
        }
        return vo;
    }

    private BigDecimal billCostAmount(List<ErpBillItem> items) {
        if (items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Long, BigDecimal> productCosts = productCostMap(items);
        return items.stream()
                .map(item -> itemCostAmount(item, productCosts))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, BigDecimal> productCostMap(List<ErpBillItem> items) {
        List<Long> productIds = items.stream().map(ErpBillItem::getProductId).filter(Objects::nonNull).distinct().toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(ErpProduct::getId, product -> nvl(product.getPurchasePrice()), (a, b) -> a));
    }

    private BigDecimal itemCostAmount(ErpBillItem item, Map<Long, BigDecimal> productCosts) {
        BigDecimal unitCost = item.getCostPrice() == null
                ? productCosts.getOrDefault(item.getProductId(), BigDecimal.ZERO)
                : item.getCostPrice();
        return nvl(item.getQty()).multiply(unitCost);
    }

    private BigDecimal optionExtraAmount(String optionIds) {
        List<Long> ids = optionAttributeIds(optionIds);
        if (ids.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return attributeService.listByIds(ids).stream()
                .map(item -> nvl(item.getExtraAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Long> optionAttributeIds(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(this::parseLong)
                .filter(Objects::nonNull)
                .toList();
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String userDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userService.getById(userId);
        if (user == null) {
            return String.valueOf(userId);
        }
        return StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private void normalizeSaleEmployeeName(ErpBill bill, ErpBill old) {
        if (bill == null || !String.valueOf(bill.getBillType()).startsWith("SALE")) {
            return;
        }
        if (StrUtil.isNotBlank(bill.getEmployeeName())) {
            bill.setEmployeeName(bill.getEmployeeName().trim());
            return;
        }
        if (old != null && StrUtil.isNotBlank(old.getEmployeeName())) {
            bill.setEmployeeName(old.getEmployeeName());
            return;
        }
        bill.setEmployeeName(userDisplayName(bill.getEmployeeId()));
    }

    private ErpBillVO.Item itemVO(ErpBillItem item) {
        ErpBillVO.Item vo = BeanUtil.copyProperties(item, ErpBillVO.Item.class);
        vo.setUnitName(masterName(unitService.getById(item.getUnitId())));
        return vo;
    }

    private void notifyNewSaleBill(ErpBill bill) {
        if (!"SALE".equals(bill.getBillType())) {
            return;
        }
        String content = bill.getBillNo() + " 已创建，请及时处理";
        String actionUrl = "/erp/sale/list?billNo=" + bill.getBillNo();
        for (SysUser user : userService.list(new QueryWrapper<SysUser>().eq("status", 1).eq("del_flag", 0).eq("internal_account", 0))) {
            if (canReceiveSaleNotification(user)) {
                notificationService.sendNotification(user.getUserId(), "新销售单", content, "warning", bill.getId(), "SALE", "ORDER", "OPEN", actionUrl);
            }
        }
    }

    private boolean canReceiveSaleNotification(SysUser user) {
        if (userService.isSuperAdmin(user)) {
            return true;
        }
        Set<String> permissions = roleService.selectPermissionsByUserId(user.getUserId());
        return permissions.contains("*:*:*") || permissions.contains("erp:sale:list");
    }

    private ErpBill requireBill(Long id, String type) {
        ErpBill bill = billService.getById(id);
        if (bill == null || !type.equals(bill.getBillType())) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return bill;
    }

    private void ensureUnaudited(ErpBill bill) {
        if (Integer.valueOf(1).equals(bill.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已审核单据不能修改或删除");
        }
        if (ErpApprovalStatus.PENDING.equals(bill.getApprovalStatus()) || ErpApprovalStatus.APPROVED.equals(bill.getApprovalStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "审批中或已审批单据不能修改或删除");
        }
    }

    private void rollbackBillIfAudited(ErpBill bill) {
        if (Integer.valueOf(1).equals(bill.getAuditStatus())) {
            auditService.unauditBill(bill.getId());
        }
    }

    private void ensureCanAudit(ErpBill bill) {
        if (Integer.valueOf(1).equals(bill.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据已审核，不能重复审核");
        }
    }

    private void validateBillReferences(ErpBill bill) {
        if ("CUSTOMER".equals(bill.getPartnerType())) {
            ErpCustomer customer = customerService.getById(bill.getPartnerId());
            if (customer == null || !Integer.valueOf(1).equals(customer.getStatus())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "客户不存在或已停用");
            }
        } else {
            ErpSupplier supplier = supplierService.getById(bill.getPartnerId());
            if (supplier == null || !Integer.valueOf(1).equals(supplier.getStatus())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "供应商不存在或已停用");
            }
        }
        requireEnabledWarehouse(bill.getWarehouseId());
        if (bill.getAccountId() != null) {
            ErpAccount account = accountService.getById(bill.getAccountId());
            if (account == null || !Integer.valueOf(1).equals(account.getStatus())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户不存在或已停用");
            }
        }
        if (positive(bill.getPaidAmount()) && bill.getAccountId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "存在收付款金额时必须选择账户");
        }
    }

    private void validatePaymentMethod(String paymentMethod, BigDecimal paidAmount) {
        if (positive(paidAmount) && StrUtil.isBlank(paymentMethod)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "存在付款金额时必须选择付款方式");
        }
        if (StrUtil.isNotBlank(paymentMethod) && !PAYMENT_METHODS.contains(paymentMethod)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "付款方式不正确");
        }
    }

    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private void requireEnabledWarehouse(Long warehouseId) {
        ErpWarehouse warehouse = warehouseService.getById(warehouseId);
        if (warehouse == null || !Integer.valueOf(1).equals(warehouse.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仓库不存在或已停用");
        }
    }

    private void ensureBillNoUnique(String billNo, Long id) {
        ErpBill exists = billService.getOne(new QueryWrapper<ErpBill>().eq("bill_no", billNo).last("limit 1"));
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
            case "SALE" -> "XS";
            case "SALE_RETURN" -> "XSTH";
            case "PURCHASE_RETURN" -> "JHTH";
            default -> "JH";
        };
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String like = prefix + "-" + date + "-";
        long count = billService.count(new QueryWrapper<ErpBill>().likeRight("bill_no", like));
        return like + String.format("%04d", count + 1);
    }

    private String flowNo(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String billType(String module) {
        return switch (module) {
            case "production" -> "SALE";
            case "sale" -> "SALE";
            case "sale-return" -> "SALE_RETURN";
            case "purchase-return" -> "PURCHASE_RETURN";
            default -> "PURCHASE";
        };
    }

    private String titleName(String module) {
        return switch (module) {
            case "production" -> "生产单";
            case "sale" -> "销售单";
            case "sale-return" -> "销售退货单";
            case "purchase-return" -> "进货退货单";
            default -> "进货单";
        };
    }

    private String partnerTitle(String billType) {
        return billType != null && billType.startsWith("SALE") ? "客户" : "供应商";
    }

    private List<String> printFields(String module, String billType) {
        List<String> fields = new ArrayList<>(List.of("单号", "日期", billType != null && billType.startsWith("SALE") ? "业务员" : partnerTitle(billType), "仓库"));
        if (billType != null && billType.startsWith("SALE")) {
            fields.addAll(List.of("客户", "收货电话", "收货地址"));
        }
        if ("production".equals(module)) {
            fields.addAll(List.of("生产进度", "快递单号", "生产人员"));
        }
        fields.addAll(List.of("应收应付", "付款方式", "付款金额", "审核状态", "备注"));
        return fields;
    }

    private void check(String module, String action) {
        StpAdminUtil.stpLogic.checkPermission("erp:" + module + ":" + action);
    }

    private void checkProductionAdmin() {
        if (isProductionFullAccessUser()) {
            return;
        }
        throw new BusinessException(ErrorCode.NO_AUTH, "生产单只能管理员维护");
    }

    private String masterName(ErpMasterData data) {
        return data == null ? null : data.getName();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String money(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String paymentStatus(BigDecimal payable, BigDecimal paid) {
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }
        return paid.compareTo(payable) >= 0 ? "PAID" : "PARTIAL";
    }
}
