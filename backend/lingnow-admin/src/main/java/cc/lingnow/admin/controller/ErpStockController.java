package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpApprovalSubmitBO;
import cc.lingnow.admin.model.bo.erp.ErpBillQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpStockCheckSaveBO;
import cc.lingnow.admin.model.enums.ErpApprovalStatus;
import cc.lingnow.admin.model.vo.erp.ErpStockCheckVO;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/erp/stock")
@RequiredArgsConstructor
public class ErpStockController {

    private final ErpStockCheckService stockCheckService;
    private final ErpStockCheckItemService stockCheckItemService;
    private final ErpStockBalanceService stockBalanceService;
    private final ErpStockFlowService stockFlowService;
    private final ErpProductService productService;
    private final ErpWarehouseService warehouseService;
    private final ErpUnitService unitService;
    private final ErpBillNoRuleService billNoRuleService;
    private final ErpDataAuthService dataAuthService;
    private final SysUserService userService;
    private final ErpApprovalService approvalService;
    private final ErpAuditService auditService;

    @GetMapping("/warning/list")
    public Result<PageResult<Map<String, Object>>> warning(@RequestParam(defaultValue = "1") Long current,
                                                           @RequestParam(defaultValue = "10") Long size,
                                                           Long productId,
                                                           Long warehouseId,
                                                           String warningType) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock:warning");
        QueryWrapper<ErpStockBalance> wrapper = new QueryWrapper<ErpStockBalance>()
                .eq(productId != null, "product_id", productId)
                .eq(warehouseId != null, "warehouse_id", warehouseId)
                .orderByDesc("update_time");
        applyWarehouseAuth(wrapper);
        IPage<ErpStockBalance> page = stockBalanceService.page(new Page<>(current, size), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(this::warningRow)
                .filter(row -> StrUtil.isBlank(warningType) || warningType.equals(row.get("warningType")))
                .toList();
        if (StrUtil.isBlank(warningType)) {
            return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
        }
        QueryWrapper<ErpStockBalance> allWrapper = new QueryWrapper<ErpStockBalance>()
                .eq(productId != null, "product_id", productId)
                .eq(warehouseId != null, "warehouse_id", warehouseId)
                .orderByDesc("update_time");
        applyWarehouseAuth(allWrapper);
        List<Map<String, Object>> allRecords = stockBalanceService.list(allWrapper)
                .stream()
                .map(this::warningRow)
                .filter(row -> warningType.equals(row.get("warningType")))
                .toList();
        int fromIndex = Math.min((int) ((current - 1) * size), allRecords.size());
        int toIndex = Math.min(fromIndex + size.intValue(), allRecords.size());
        return Result.success(PageResult.of(current, size, (long) allRecords.size(), allRecords.subList(fromIndex, toIndex)));
    }

    @GetMapping("/check/list")
    public Result<PageResult<ErpStockCheckVO>> checkList(ErpBillQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock-check:list");
        QueryWrapper<ErpStockCheck> wrapper = new QueryWrapper<ErpStockCheck>()
                .like(StrUtil.isNotBlank(query.getBillNo()), "check_no", query.getBillNo())
                .eq(query.getAuditStatus() != null, "audit_status", query.getAuditStatus())
                .ge(query.getBeginDate() != null, "check_date", query.getBeginDate())
                .le(query.getEndDate() != null, "check_date", query.getEndDate())
                .orderByDesc("check_date")
                .orderByDesc("create_time");
        applyWarehouseAuth(wrapper);
        IPage<ErpStockCheck> page = stockCheckService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(item -> toVO(item, false)).toList()));
    }

    private <T> void applyWarehouseAuth(QueryWrapper<T> wrapper) {
        if (isAdminUser()) {
            return;
        }
        List<Long> warehouseIds = dataAuthService.authorizedIds(currentUserId(), "WAREHOUSE");
        if (!warehouseIds.isEmpty()) {
            wrapper.in("warehouse_id", warehouseIds);
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

    @GetMapping("/check/nextNo")
    public Result<String> nextCheckNo() {
        StpAdminUtil.stpLogic.checkPermission("erp:stock-check:add");
        return Result.success(nextNo());
    }

    @GetMapping("/check/{id}")
    public Result<ErpStockCheckVO> getCheck(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock-check:list");
        return Result.success(toVO(requireCheck(id), true));
    }

    @PostMapping("/check")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP库存盘点", businessType = BusinessType.INSERT)
    public Result<Void> addCheck(@Valid @RequestBody ErpStockCheckSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock-check:add");
        ErpStockCheck check = buildCheck(bo);
        check.setCheckNo(StrUtil.isBlank(bo.getCheckNo()) ? nextNo() : bo.getCheckNo());
        ensureNoUnique(check.getCheckNo(), null);
        stockCheckService.save(check);
        saveItems(check, bo.getItems());
        return Result.success();
    }

    @PutMapping("/check")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP库存盘点", businessType = BusinessType.UPDATE)
    public Result<Void> editCheck(@Valid @RequestBody ErpStockCheckSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        StpAdminUtil.stpLogic.checkPermission("erp:stock-check:edit");
        ErpStockCheck old = requireCheck(bo.getId());
        ensureUnaudited(old);
        ErpStockCheck check = buildCheck(bo);
        check.setId(old.getId());
        check.setCheckNo(StrUtil.isBlank(bo.getCheckNo()) ? old.getCheckNo() : bo.getCheckNo());
        ensureNoUnique(check.getCheckNo(), check.getId());
        stockCheckService.updateById(check);
        stockCheckItemService.remove(new QueryWrapper<ErpStockCheckItem>().eq("check_id", check.getId()));
        saveItems(check, bo.getItems());
        return Result.success();
    }

    @DeleteMapping("/check/{ids}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP库存盘点", businessType = BusinessType.DELETE)
    public Result<Void> removeCheck(@PathVariable List<Long> ids) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock-check:remove");
        for (Long id : ids) {
            ensureUnaudited(requireCheck(id));
            stockCheckItemService.remove(new QueryWrapper<ErpStockCheckItem>().eq("check_id", id));
        }
        stockCheckService.removeByIds(ids);
        return Result.success();
    }

    @PutMapping("/check/audit/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP库存盘点审核", businessType = BusinessType.UPDATE)
    public Result<Void> auditCheck(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock-check:audit");
        ErpApprovalSubmitBO bo = new ErpApprovalSubmitBO();
        bo.setBizType("STOCK_CHECK");
        bo.setBizId(id);
        approvalService.submit(bo);
        return Result.success();
    }

    @PutMapping("/check/unaudit/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP库存盘点反审核", businessType = BusinessType.UPDATE)
    public Result<Void> unauditCheck(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock-check:unaudit");
        ErpStockCheck check = requireCheck(id);
        auditService.unauditStockCheck(id);
        check.setAuditStatus(0);
        check.setAuditTime(null);
        check.setAuditBy(null);
        check.setApprovalStatus(ErpApprovalStatus.NONE);
        check.setApprovalFinishTime(null);
        stockCheckService.updateById(check);
        return Result.success();
    }

    private ErpStockCheck buildCheck(ErpStockCheckSaveBO bo) {
        List<ErpStockCheckItem> items = buildItems(null, bo.getWarehouseId(), bo.getItems());
        ErpStockCheck check = BeanUtil.copyProperties(bo, ErpStockCheck.class);
        requireEnabledWarehouse(check.getWarehouseId());
        check.setTotalProfitQty(total(items, true, false));
        check.setTotalLossQty(total(items, false, false));
        check.setTotalProfitAmount(total(items, true, true));
        check.setTotalLossAmount(total(items, false, true));
        check.setAuditStatus(0);
        check.setApprovalStatus(ErpApprovalStatus.NONE);
        return check;
    }

    private void saveItems(ErpStockCheck check, List<ErpStockCheckSaveBO.Item> sourceItems) {
        stockCheckItemService.saveBatch(buildItems(check.getId(), check.getWarehouseId(), sourceItems));
    }

    private List<ErpStockCheckItem> buildItems(Long checkId, Long warehouseId, List<ErpStockCheckSaveBO.Item> sourceItems) {
        return sourceItems.stream().map(source -> {
            ErpProduct product = productService.getById(source.getProductId());
            if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品不存在或已停用");
            }
            ErpStockBalance balance = balance(product.getId(), warehouseId);
            BigDecimal bookQty = balance == null ? BigDecimal.ZERO : nvl(balance.getQty());
            BigDecimal checkQty = nvl(source.getCheckQty());
            if (checkQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "盘点数量不能小于0");
            }
            BigDecimal costPrice = balance == null ? nvl(product.getPurchasePrice()) : nvl(balance.getAvgCost());
            BigDecimal diffQty = checkQty.subtract(bookQty);
            ErpStockCheckItem item = new ErpStockCheckItem();
            item.setCheckId(checkId);
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setSpec(product.getSpec());
            item.setUnitId(product.getUnitId());
            item.setWarehouseId(warehouseId);
            item.setBookQty(bookQty);
            item.setCheckQty(checkQty);
            item.setDiffQty(diffQty);
            item.setCostPrice(costPrice);
            item.setDiffAmount(diffQty.multiply(costPrice).setScale(4, RoundingMode.HALF_UP));
            item.setRemark(source.getRemark());
            return item;
        }).toList();
    }

    private void changeStock(ErpStockCheck check, ErpStockCheckItem item) {
        if (nvl(item.getDiffQty()).compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        ErpStockBalance balance = balance(item.getProductId(), item.getWarehouseId());
        if (balance == null) {
            balance = new ErpStockBalance();
            balance.setProductId(item.getProductId());
            balance.setWarehouseId(item.getWarehouseId());
            balance.setQty(BigDecimal.ZERO);
            balance.setCostAmount(BigDecimal.ZERO);
            balance.setAvgCost(nvl(item.getCostPrice()));
        }
        BigDecimal before = nvl(balance.getQty());
        BigDecimal beforeAmount = nvl(balance.getCostAmount());
        BigDecimal after = before.add(item.getDiffQty());
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, item.getProductName() + "盘亏后库存不能为负");
        }
        BigDecimal afterAmount = beforeAmount.add(nvl(item.getDiffAmount()));
        if (after.compareTo(BigDecimal.ZERO) == 0) {
            afterAmount = BigDecimal.ZERO;
        }
        balance.setQty(after);
        balance.setCostAmount(afterAmount);
        balance.setAvgCost(after.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : afterAmount.divide(after, 4, RoundingMode.HALF_UP));
        if (balance.getId() == null) {
            stockBalanceService.save(balance);
        } else {
            stockBalanceService.updateById(balance);
        }
        ErpStockFlow flow = new ErpStockFlow();
        flow.setFlowNo(flowNo());
        flow.setSourceBillId(check.getId());
        flow.setSourceBillNo(check.getCheckNo());
        flow.setSourceBillType("STOCK_CHECK");
        flow.setProductId(item.getProductId());
        flow.setWarehouseId(item.getWarehouseId());
        flow.setDirection(item.getDiffQty().compareTo(BigDecimal.ZERO) > 0 ? "IN" : "OUT");
        flow.setQty(item.getDiffQty().abs());
        flow.setPrice(item.getCostPrice());
        flow.setAmount(item.getDiffAmount().abs());
        flow.setBeforeQty(before);
        flow.setAfterQty(after);
        flow.setOperateTime(LocalDateTime.now());
        stockFlowService.save(flow);
    }

    private void rollbackStock(ErpStockCheck check) {
        List<ErpStockFlow> flows = stockFlowService.list(new QueryWrapper<ErpStockFlow>()
                .eq("source_bill_id", check.getId()).eq("source_bill_type", "STOCK_CHECK").orderByDesc("create_time"));
        for (ErpStockFlow flow : flows) {
            ErpStockBalance balance = balance(flow.getProductId(), flow.getWarehouseId());
            if (balance == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "库存余额不存在，无法反审核");
            }
            BigDecimal after = "IN".equals(flow.getDirection()) ? balance.getQty().subtract(flow.getQty()) : balance.getQty().add(flow.getQty());
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "反审核后库存不能为负");
            }
            BigDecimal afterAmount = "IN".equals(flow.getDirection()) ? nvl(balance.getCostAmount()).subtract(nvl(flow.getAmount())) : nvl(balance.getCostAmount()).add(nvl(flow.getAmount()));
            if (after.compareTo(BigDecimal.ZERO) == 0) {
                afterAmount = BigDecimal.ZERO;
            }
            balance.setQty(after);
            balance.setCostAmount(afterAmount);
            balance.setAvgCost(after.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : afterAmount.divide(after, 4, RoundingMode.HALF_UP));
            stockBalanceService.updateById(balance);
        }
        stockFlowService.remove(new QueryWrapper<ErpStockFlow>().eq("source_bill_id", check.getId()).eq("source_bill_type", "STOCK_CHECK"));
    }

    private ErpStockCheckVO toVO(ErpStockCheck check, boolean withItems) {
        ErpStockCheckVO vo = BeanUtil.copyProperties(check, ErpStockCheckVO.class);
        vo.setWarehouseName(masterName(warehouseService.getById(check.getWarehouseId())));
        if (withItems) {
            vo.setItems(stockCheckItemService.list(new QueryWrapper<ErpStockCheckItem>().eq("check_id", check.getId())).stream().map(this::itemVO).toList());
        }
        return vo;
    }

    private ErpStockCheckVO.Item itemVO(ErpStockCheckItem item) {
        ErpStockCheckVO.Item vo = BeanUtil.copyProperties(item, ErpStockCheckVO.Item.class);
        vo.setUnitName(masterName(unitService.getById(item.getUnitId())));
        return vo;
    }

    private Map<String, Object> warningRow(ErpStockBalance balance) {
        ErpProduct product = productService.getById(balance.getProductId());
        BigDecimal qty = nvl(balance.getQty());
        BigDecimal minStock = product == null ? BigDecimal.ZERO : nvl(product.getMinStock());
        BigDecimal maxStock = product == null ? BigDecimal.ZERO : nvl(product.getMaxStock());
        String warningType = qty.compareTo(minStock) < 0 ? "LOW" : maxStock.compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(maxStock) > 0 ? "HIGH" : "NORMAL";
        Map<String, Object> row = new HashMap<>();
        row.put("id", balance.getId());
        row.put("productId", balance.getProductId());
        row.put("productCode", product == null ? "" : product.getCode());
        row.put("productName", product == null ? "" : product.getName());
        row.put("warehouseId", balance.getWarehouseId());
        row.put("warehouseName", masterName(warehouseService.getById(balance.getWarehouseId())));
        row.put("qty", qty);
        row.put("minStock", minStock);
        row.put("maxStock", maxStock);
        row.put("warningType", warningType);
        row.put("costAmount", nvl(balance.getCostAmount()));
        return row;
    }

    private BigDecimal total(List<ErpStockCheckItem> items, boolean profit, boolean amount) {
        return items.stream()
                .filter(item -> profit ? item.getDiffQty().compareTo(BigDecimal.ZERO) > 0 : item.getDiffQty().compareTo(BigDecimal.ZERO) < 0)
                .map(item -> amount ? item.getDiffAmount().abs() : item.getDiffQty().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ErpStockCheck requireCheck(Long id) {
        ErpStockCheck check = stockCheckService.getById(id);
        if (check == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return check;
    }

    private void ensureUnaudited(ErpStockCheck check) {
        if (Integer.valueOf(1).equals(check.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已审核盘点单不能修改或删除");
        }
        if (ErpApprovalStatus.PENDING.equals(check.getApprovalStatus()) || ErpApprovalStatus.APPROVED.equals(check.getApprovalStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "审批中或已审批盘点单不能修改或删除");
        }
    }

    private void ensureCanAudit(ErpStockCheck check) {
        if (Integer.valueOf(1).equals(check.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "盘点单已审核，不能重复审核");
        }
    }

    private void ensureNoUnique(String checkNo, Long id) {
        ErpStockCheck exists = stockCheckService.getOne(new QueryWrapper<ErpStockCheck>().eq("check_no", checkNo).last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "盘点单号已存在");
        }
    }

    private ErpStockBalance balance(Long productId, Long warehouseId) {
        return stockBalanceService.getOne(new QueryWrapper<ErpStockBalance>()
                .eq("product_id", productId).eq("warehouse_id", warehouseId).last("limit 1"));
    }

    private void requireEnabledWarehouse(Long warehouseId) {
        ErpWarehouse warehouse = warehouseService.getById(warehouseId);
        if (warehouse == null || !Integer.valueOf(1).equals(warehouse.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仓库不存在或已停用");
        }
    }

    private String nextNo() {
        String ruleNo = billNoRuleService.nextNo("STOCK_CHECK");
        if (StrUtil.isNotBlank(ruleNo)) {
            return ruleNo;
        }
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String like = "PD-" + date + "-";
        long count = stockCheckService.count(new QueryWrapper<ErpStockCheck>().likeRight("check_no", like));
        return like + String.format("%04d", count + 1);
    }

    private String flowNo() {
        return "ST-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String masterName(ErpMasterData data) {
        return data == null ? null : data.getName();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
