package cc.lingnow.admin.controller;

import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/erp/report")
@RequiredArgsConstructor
public class ErpReportController {

    private final ErpProductService productService;
    private final ErpCustomerService customerService;
    private final ErpSupplierService supplierService;
    private final ErpBillService billService;
    private final ErpBillItemService billItemService;
    private final ErpStockBalanceService stockBalanceService;
    private final ErpStockFlowService stockFlowService;
    private final ErpFundFlowService fundFlowService;
    private final ErpPartnerFlowService partnerFlowService;
    private final ErpAccountService accountService;
    private final ErpWarehouseService warehouseService;

    @GetMapping("/stock-balance")
    public Result<PageResult<Map<String, Object>>> stockBalance(@RequestParam(defaultValue = "1") Long current,
                                                                @RequestParam(defaultValue = "10") Long size,
                                                                Long productId,
                                                                Long warehouseId) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock:balance");
        QueryWrapper<ErpStockBalance> wrapper = new QueryWrapper<ErpStockBalance>()
                .eq(productId != null, "product_id", productId)
                .eq(warehouseId != null, "warehouse_id", warehouseId)
                .orderByDesc("update_time");
        IPage<ErpStockBalance> page = stockBalanceService.page(new Page<>(current, size), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::stockBalanceRow).toList()));
    }

    @GetMapping("/stock-flow")
    public Result<PageResult<Map<String, Object>>> stockFlow(@RequestParam(defaultValue = "1") Long current,
                                                             @RequestParam(defaultValue = "10") Long size,
                                                             Long productId,
                                                             Long warehouseId,
                                                             String direction) {
        StpAdminUtil.stpLogic.checkPermission("erp:stock:flow");
        QueryWrapper<ErpStockFlow> wrapper = new QueryWrapper<ErpStockFlow>()
                .eq(productId != null, "product_id", productId)
                .eq(warehouseId != null, "warehouse_id", warehouseId)
                .eq(direction != null && !direction.isBlank(), "direction", direction)
                .orderByDesc("operate_time");
        IPage<ErpStockFlow> page = stockFlowService.page(new Page<>(current, size), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::stockFlowRow).toList()));
    }

    @GetMapping("/bill-detail")
    public Result<PageResult<Map<String, Object>>> billDetail(@RequestParam(defaultValue = "1") Long current,
                                                              @RequestParam(defaultValue = "10") Long size,
                                                              String billType,
                                                              Long productId) {
        StpAdminUtil.stpLogic.checkPermission("SALE".equals(billType) ? "erp:report:sale-detail" : "erp:report:purchase-detail");
        List<Long> billIds = billService.list(new QueryWrapper<ErpBill>().eq("bill_type", billType)).stream().map(ErpBill::getId).toList();
        QueryWrapper<ErpBillItem> wrapper = new QueryWrapper<ErpBillItem>()
                .in(!billIds.isEmpty(), "bill_id", billIds)
                .eq(productId != null, "product_id", productId)
                .orderByDesc("create_time");
        if (billIds.isEmpty()) {
            return Result.success(PageResult.of(current, size, 0L, List.of()));
        }
        IPage<ErpBillItem> page = billItemService.page(new Page<>(current, size), wrapper);
        Map<Long, ErpBill> billMap = billService.listByIds(billIds).stream()
                .collect(Collectors.toMap(ErpBill::getId, Function.identity(), (oldValue, newValue) -> oldValue));
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(item -> billDetailRow(item, billMap.get(item.getBillId()))).toList()));
    }

    @GetMapping("/partner-balance")
    public Result<List<Map<String, Object>>> partnerBalance() {
        StpAdminUtil.stpLogic.checkPermission("erp:report:partner-balance");
        Map<String, Map<String, Object>> map = new HashMap<>();
        for (ErpPartnerFlow flow : partnerFlowService.list()) {
            String key = flow.getPartnerType() + ":" + flow.getPartnerId();
            Map<String, Object> row = map.computeIfAbsent(key, k -> new HashMap<>());
            row.put("partnerId", flow.getPartnerId());
            row.put("partnerType", flow.getPartnerType());
            row.put("partnerName", partnerName(flow.getPartnerType(), flow.getPartnerId()));
            BigDecimal receivable = amount(row.get("receivable"));
            BigDecimal payable = amount(row.get("payable"));
            if ("RECEIVABLE".equals(flow.getDirection())) {
                receivable = receivable.add(flow.getAmount());
            } else if ("RECEIVE".equals(flow.getDirection())) {
                receivable = receivable.subtract(flow.getAmount());
            } else if ("PAYABLE".equals(flow.getDirection())) {
                payable = payable.add(flow.getAmount());
            } else if ("PAY".equals(flow.getDirection())) {
                payable = payable.subtract(flow.getAmount());
            }
            row.put("receivable", receivable);
            row.put("payable", payable);
        }
        return Result.success(map.values().stream().toList());
    }

    @GetMapping("/account-balance")
    public Result<List<Map<String, Object>>> accountBalance() {
        StpAdminUtil.stpLogic.checkPermission("erp:report:account-balance");
        return Result.success(accountService.list().stream().map(account -> {
            BigDecimal balance = account.getOpeningBalance();
            for (ErpFundFlow flow : fundFlowService.list(new QueryWrapper<ErpFundFlow>().eq("account_id", account.getId()))) {
                balance = "IN".equals(flow.getDirection()) ? balance.add(flow.getAmount()) : balance.subtract(flow.getAmount());
            }
            Map<String, Object> row = new HashMap<>();
            row.put("accountId", account.getId());
            row.put("accountName", account.getName());
            row.put("balance", balance);
            return row;
        }).toList());
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        StpAdminUtil.stpLogic.checkPermission("erp:report:summary");
        LocalDate today = LocalDate.now();
        Map<String, Object> data = new HashMap<>();
        data.put("productCount", productService.count());
        data.put("customerCount", customerService.count());
        data.put("supplierCount", supplierService.count());
        data.put("todaySaleAmount", billAmount("SALE", today));
        data.put("todayPurchaseAmount", billAmount("PURCHASE", today));
        data.put("stockAmount", stockBalanceService.list().stream().map(ErpStockBalance::getCostAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        data.put("receivable", partnerTotal("RECEIVABLE").subtract(partnerTotal("RECEIVE")));
        data.put("payable", partnerTotal("PAYABLE").subtract(partnerTotal("PAY")));
        data.put("accountBalance", accountBalance().getData().stream().map(row -> amount(row.get("balance"))).reduce(BigDecimal.ZERO, BigDecimal::add));
        return Result.success(data);
    }

    private BigDecimal billAmount(String billType, LocalDate date) {
        return billService.list(new QueryWrapper<ErpBill>().eq("bill_type", billType).eq("bill_date", date).eq("audit_status", 1))
                .stream().map(ErpBill::getPayableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal partnerTotal(String direction) {
        return partnerFlowService.list(new QueryWrapper<ErpPartnerFlow>().eq("direction", direction))
                .stream().map(ErpPartnerFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal amount(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    private Map<String, Object> stockBalanceRow(ErpStockBalance balance) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", balance.getId());
        row.put("productId", balance.getProductId());
        row.put("productName", productName(balance.getProductId()));
        row.put("warehouseId", balance.getWarehouseId());
        row.put("warehouseName", masterName(warehouseService.getById(balance.getWarehouseId())));
        row.put("qty", balance.getQty());
        row.put("avgCost", balance.getAvgCost());
        row.put("costAmount", balance.getCostAmount());
        row.put("updateTime", balance.getUpdateTime());
        return row;
    }

    private Map<String, Object> stockFlowRow(ErpStockFlow flow) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", flow.getId());
        row.put("flowNo", flow.getFlowNo());
        row.put("operateTime", flow.getOperateTime());
        row.put("sourceBillNo", flow.getSourceBillNo());
        row.put("sourceBillType", flow.getSourceBillType());
        row.put("productId", flow.getProductId());
        row.put("productName", productName(flow.getProductId()));
        row.put("warehouseId", flow.getWarehouseId());
        row.put("warehouseName", masterName(warehouseService.getById(flow.getWarehouseId())));
        row.put("direction", flow.getDirection());
        row.put("qty", flow.getQty());
        row.put("price", flow.getPrice());
        row.put("amount", flow.getAmount());
        row.put("beforeQty", flow.getBeforeQty());
        row.put("afterQty", flow.getAfterQty());
        return row;
    }

    private Map<String, Object> billDetailRow(ErpBillItem item, ErpBill bill) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", item.getId());
        row.put("billId", item.getBillId());
        row.put("billNo", bill == null ? null : bill.getBillNo());
        row.put("billDate", bill == null ? null : bill.getBillDate());
        row.put("partnerType", bill == null ? null : bill.getPartnerType());
        row.put("partnerName", bill == null ? null : partnerName(bill.getPartnerType(), bill.getPartnerId()));
        row.put("productId", item.getProductId());
        row.put("productCode", item.getProductCode());
        row.put("productName", item.getProductName());
        row.put("spec", item.getSpec());
        row.put("warehouseName", masterName(warehouseService.getById(item.getWarehouseId())));
        row.put("qty", item.getQty());
        row.put("price", item.getPrice());
        row.put("amount", item.getAmount());
        row.put("discountAmount", item.getDiscountAmount());
        row.put("finalAmount", item.getFinalAmount());
        return row;
    }

    private String partnerName(String partnerType, Long partnerId) {
        if ("CUSTOMER".equals(partnerType)) {
            return masterName(customerService.getById(partnerId));
        }
        if ("SUPPLIER".equals(partnerType)) {
            return masterName(supplierService.getById(partnerId));
        }
        return null;
    }

    private String productName(Long productId) {
        ErpProduct product = productService.getById(productId);
        return product == null ? null : product.getName();
    }

    private String masterName(ErpMasterData data) {
        return data == null ? null : data.getName();
    }
}
