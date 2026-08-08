package cc.lingnow.admin.controller;

import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.admin.util.CsvExportUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
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
    private final ErpProductAttributeService attributeService;
    private final SysUserService userService;

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
            for (ErpFundFlow flow : fundFlowService.list(new QueryWrapper<ErpFundFlow>().eq("account_id", account.getId()).eq("del_flag", 0))) {
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

    @GetMapping("/bill-stat")
    public Result<List<Map<String, Object>>> billStat(String billType,
                                                      String groupBy,
                                                      LocalDate beginDate,
                                                      LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:" + ("SALE".equals(billType) ? "sale-stat" : "purchase-stat"));
        return Result.success(groupBills(billType, groupBy, beginDate, endDate));
    }

    @GetMapping("/profit")
    public Result<List<Map<String, Object>>> profit(String groupBy, LocalDate beginDate, LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:profit");
        List<ErpBill> saleBills = billService.list(billWrapper("SALE", beginDate, endDate));
        Map<Long, ErpBill> billMap = saleBills.stream().collect(Collectors.toMap(ErpBill::getId, Function.identity(), (a, b) -> a));
        if (billMap.isEmpty()) {
            return Result.success(List.of());
        }
        List<Long> saleIds = new ArrayList<>(billMap.keySet());
        List<ErpBillItem> items = billItemService.list(new QueryWrapper<ErpBillItem>().in("bill_id", saleIds));
        Map<Long, BigDecimal> productCosts = productCostMap(items);
        Map<Long, BigDecimal> optionExtras = optionExtraAmountMap(items);
        Map<String, Map<String, Object>> rows = new HashMap<>();
        for (ErpBillItem item : items) {
            ErpBill bill = billMap.get(item.getBillId());
            BigDecimal costAmount = itemCostAmount(item, productCosts, optionExtras);
            String key = profitKey(groupBy, bill, item);
            Map<String, Object> row = rows.computeIfAbsent(key, k -> baseProfitRow(groupBy, bill, item));
            add(row, "saleAmount", item.getFinalAmount());
            add(row, "costAmount", costAmount);
            add(row, "profitAmount", item.getFinalAmount().subtract(costAmount));
            add(row, "qty", item.getQty());
        }
        return Result.success(sortAmount(rows.values().stream().toList(), "profitAmount"));
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(LocalDate beginDate, LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:sale-analysis");
        Map<LocalDate, Map<String, Object>> rows = new HashMap<>();
        for (ErpBill bill : billService.list(new QueryWrapper<ErpBill>()
                .in("bill_type", List.of("SALE", "SALE_RETURN", "PURCHASE", "PURCHASE_RETURN"))
                .ge(beginDate != null, "bill_date", beginDate)
                .le(endDate != null, "bill_date", endDate))) {
            Map<String, Object> row = rows.computeIfAbsent(bill.getBillDate(), date -> {
                Map<String, Object> created = new HashMap<>();
                created.put("date", date);
                created.put("saleAmount", BigDecimal.ZERO);
                created.put("saleReturnAmount", BigDecimal.ZERO);
                created.put("purchaseAmount", BigDecimal.ZERO);
                created.put("purchaseReturnAmount", BigDecimal.ZERO);
                return created;
            });
            switch (bill.getBillType()) {
                case "SALE" -> add(row, "saleAmount", bill.getPayableAmount());
                case "SALE_RETURN" -> add(row, "saleReturnAmount", bill.getPayableAmount());
                case "PURCHASE" -> add(row, "purchaseAmount", bill.getPayableAmount());
                case "PURCHASE_RETURN" -> add(row, "purchaseReturnAmount", bill.getPayableAmount());
                default -> {
                }
            }
        }
        return Result.success(rows.values().stream().sorted(Comparator.comparing(row -> (LocalDate) row.get("date"))).toList());
    }

    @GetMapping("/business-profit")
    public Result<Map<String, Object>> businessProfit(LocalDate beginDate, LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:business-profit");
        BigDecimal saleAmount = billTotal("SALE", beginDate, endDate);
        BigDecimal saleReturnAmount = billTotal("SALE_RETURN", beginDate, endDate);
        BigDecimal purchaseAmount = billTotal("PURCHASE", beginDate, endDate);
        BigDecimal purchaseReturnAmount = billTotal("PURCHASE_RETURN", beginDate, endDate);
        BigDecimal otherIncome = financeTotal("INCOME", beginDate, endDate);
        BigDecimal otherExpense = financeTotal("EXPENSE", beginDate, endDate);
        Map<String, Object> row = new HashMap<>();
        row.put("saleAmount", saleAmount);
        row.put("saleReturnAmount", saleReturnAmount);
        row.put("purchaseAmount", purchaseAmount);
        row.put("purchaseReturnAmount", purchaseReturnAmount);
        row.put("otherIncome", otherIncome);
        row.put("otherExpense", otherExpense);
        row.put("grossProfit", saleAmount.subtract(saleReturnAmount).subtract(purchaseAmount).add(purchaseReturnAmount));
        row.put("netProfit", saleAmount.subtract(saleReturnAmount).subtract(purchaseAmount).add(purchaseReturnAmount).add(otherIncome).subtract(otherExpense));
        return Result.success(row);
    }

    @GetMapping("/hot-products")
    public Result<List<Map<String, Object>>> hotProducts(LocalDate beginDate, LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:hot-products");
        List<ErpBill> saleBills = billService.list(billWrapper("SALE", beginDate, endDate));
        if (saleBills.isEmpty()) {
            return Result.success(List.of());
        }
        List<Long> billIds = saleBills.stream().map(ErpBill::getId).toList();
        Map<Long, ErpBill> billMap = saleBills.stream().collect(Collectors.toMap(ErpBill::getId, Function.identity(), (a, b) -> a));
        Map<Long, Map<String, Object>> rows = new HashMap<>();
        for (ErpBillItem item : billItemService.list(new QueryWrapper<ErpBillItem>().in("bill_id", billIds))) {
            Map<String, Object> row = rows.computeIfAbsent(item.getProductId(), id -> productRow(item));
            add(row, "qty", item.getQty());
            add(row, "saleAmount", item.getFinalAmount());
            row.put("lastBillDate", billMap.get(item.getBillId()).getBillDate());
        }
        return Result.success(sortAmount(rows.values().stream().toList(), "qty"));
    }

    @GetMapping("/employee-performance")
    public Result<List<Map<String, Object>>> employeePerformance(LocalDate beginDate, LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:employee-performance");
        List<ErpBill> bills = billService.list(new QueryWrapper<ErpBill>()
                .in("bill_type", List.of("SALE", "SALE_RETURN"))
                .ge(beginDate != null, "bill_date", beginDate)
                .le(endDate != null, "bill_date", endDate));
        Map<String, BigDecimal> costAmounts = billCostAmounts(bills);
        Map<String, Map<String, Object>> rows = new HashMap<>();
        for (ErpBill bill : bills) {
            String employeeKey = employeeKey(bill);
            BigDecimal paidAmount = amount(bill.getPaidAmount());
            BigDecimal costAmount = costAmounts.getOrDefault(billCostKey(bill), BigDecimal.ZERO);
            Map<String, Object> row = rows.computeIfAbsent(employeeKey, key -> {
                Map<String, Object> created = new HashMap<>();
                created.put("employeeId", bill.getEmployeeId());
                created.put("employeeName", employeeDisplayName(bill));
                created.put("saleAmount", BigDecimal.ZERO);
                created.put("returnAmount", BigDecimal.ZERO);
                created.put("netAmount", BigDecimal.ZERO);
                created.put("commissionAmount", BigDecimal.ZERO);
                return created;
            });
            row.put("employeeName", preferEmployeeName(Objects.toString(row.get("employeeName"), ""), employeeDisplayName(bill)));
            if ("SALE".equals(bill.getBillType())) {
                add(row, "saleAmount", paidAmount);
                add(row, "netAmount", paidAmount);
                add(row, "commissionAmount", paidAmount.subtract(costAmount));
            } else {
                add(row, "returnAmount", paidAmount);
                add(row, "netAmount", paidAmount.negate());
                add(row, "commissionAmount", costAmount.subtract(paidAmount));
            }
        }
        return Result.success(sortAmount(rows.values().stream().toList(), "netAmount"));
    }

    @GetMapping("/employee-performance/sale-details")
    public Result<Map<String, Object>> employeeSaleDetails(Long employeeId,
                                                           String employeeName,
                                                           LocalDate beginDate,
                                                           LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:employee-performance");
        QueryWrapper<ErpBill> wrapper = new QueryWrapper<ErpBill>()
                .eq("bill_type", "SALE")
                .eq(employeeId != null, "employee_id", employeeId)
                .eq(employeeId == null && StrUtil.isNotBlank(employeeName), "employee_name", employeeName)
                .ge(beginDate != null, "bill_date", beginDate)
                .le(endDate != null, "bill_date", endDate)
                .orderByDesc("bill_date")
                .orderByDesc("create_time");
        if (employeeId == null && StrUtil.isBlank(employeeName)) {
            wrapper.isNull("employee_id").and(item -> item.isNull("employee_name").or().eq("employee_name", ""));
        }
        List<ErpBill> bills = billService.list(wrapper);
        Map<String, BigDecimal> costAmounts = billCostAmounts(bills);
        List<Map<String, Object>> records = bills.stream().map(bill -> employeeSaleDetailRow(bill, costAmounts)).toList();
        Map<String, Object> summary = new HashMap<>();
        summary.put("employeeId", employeeId);
        summary.put("employeeName", employeeDetailName(employeeId, employeeName, bills));
        summary.put("billCount", records.size());
        summary.put("orderAmount", records.stream().map(row -> amount(row.get("orderAmount"))).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("saleAmount", records.stream().map(row -> amount(row.get("saleAmount"))).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("paidAmount", records.stream().map(row -> amount(row.get("paidAmount"))).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("costAmount", records.stream().map(row -> amount(row.get("costAmount"))).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("profitAmount", records.stream().map(row -> amount(row.get("profitAmount"))).reduce(BigDecimal.ZERO, BigDecimal::add));
        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("records", records);
        return Result.success(result);
    }

    @GetMapping("/stock-summary")
    public Result<List<Map<String, Object>>> stockSummary(LocalDate beginDate, LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:stock-summary");
        Map<String, Map<String, Object>> rows = new HashMap<>();
        for (ErpStockFlow flow : stockFlowService.list(new QueryWrapper<ErpStockFlow>()
                .ge(beginDate != null, "operate_time", beginDate == null ? null : beginDate.atStartOfDay())
                .lt(endDate != null, "operate_time", endDate == null ? null : endDate.plusDays(1).atStartOfDay()))) {
            String key = flow.getProductId() + ":" + flow.getWarehouseId();
            Map<String, Object> row = rows.computeIfAbsent(key, k -> {
                Map<String, Object> created = new HashMap<>();
                created.put("productId", flow.getProductId());
                created.put("productName", productName(flow.getProductId()));
                created.put("warehouseId", flow.getWarehouseId());
                created.put("warehouseName", masterName(warehouseService.getById(flow.getWarehouseId())));
                created.put("inQty", BigDecimal.ZERO);
                created.put("outQty", BigDecimal.ZERO);
                created.put("inAmount", BigDecimal.ZERO);
                created.put("outAmount", BigDecimal.ZERO);
                created.put("netQty", BigDecimal.ZERO);
                return created;
            });
            if ("IN".equals(flow.getDirection())) {
                add(row, "inQty", flow.getQty());
                add(row, "inAmount", flow.getAmount());
                add(row, "netQty", flow.getQty());
            } else {
                add(row, "outQty", flow.getQty());
                add(row, "outAmount", flow.getAmount());
                add(row, "netQty", flow.getQty().negate());
            }
        }
        return Result.success(rows.values().stream().toList());
    }

    @GetMapping("/inventory-change")
    public Result<List<Map<String, Object>>> inventoryChange(LocalDate beginDate, LocalDate endDate) {
        StpAdminUtil.stpLogic.checkPermission("erp:report:inventory-change");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : stockSummary(beginDate, endDate).getData()) {
            Map<String, Object> changed = new HashMap<>(row);
            changed.put("currentQty", stockBalanceService.list(new QueryWrapper<ErpStockBalance>()
                            .eq("product_id", row.get("productId")).eq("warehouse_id", row.get("warehouseId"))).stream()
                    .map(ErpStockBalance::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
            rows.add(changed);
        }
        return Result.success(rows);
    }

    @GetMapping("/export")
    public void export(String reportCode,
                       String billType,
                       String groupBy,
                       LocalDate beginDate,
                       LocalDate endDate,
                       HttpServletResponse response) throws Exception {
        ReportExport export = reportExport(reportCode, billType, groupBy, beginDate, endDate);
        List<List<String>> rows = export.rows().stream()
                .map(row -> export.columns().keySet().stream().map(key -> text(row.get(key))).toList())
                .toList();
        CsvExportUtil.write(response, export.title() + ".csv", export.columns().values().stream().toList(), rows);
    }

    private BigDecimal billAmount(String billType, LocalDate date) {
        return billService.list(new QueryWrapper<ErpBill>().eq("bill_type", billType).eq("bill_date", date))
                .stream().map(ErpBill::getPayableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ReportExport reportExport(String reportCode, String billType, String groupBy, LocalDate beginDate, LocalDate endDate) {
        String code = Objects.toString(reportCode, "");
        return switch (code) {
            case "sale-stat" -> new ReportExport("销售统计", statColumns("销售额"), groupBills("SALE", blankDefault(groupBy, "date"), beginDate, endDate));
            case "purchase-stat" -> new ReportExport("进货统计", statColumns("进货额"), groupBills("PURCHASE", blankDefault(groupBy, "date"), beginDate, endDate));
            case "sale-profit-product" -> new ReportExport("销售利润表-按商品", profitColumns(), profit("product", beginDate, endDate).getData());
            case "sale-profit-bill" -> new ReportExport("销售利润表-按单据", profitColumns(), profit("bill", beginDate, endDate).getData());
            case "sale-profit-customer" -> new ReportExport("销售利润表-按客户", profitColumns(), profit("customer", beginDate, endDate).getData());
            case "sale-analysis" -> new ReportExport("销售分析", trendColumns(), trend(beginDate, endDate).getData());
            case "business-profit" -> new ReportExport("经营利润", profitSummaryColumns(), List.of(businessProfit(beginDate, endDate).getData()));
            case "hot-products" -> new ReportExport("商品热销榜", hotProductColumns(), hotProducts(beginDate, endDate).getData());
            case "employee-performance" -> new ReportExport("业务员业绩统计", employeeColumns(true), employeePerformance(beginDate, endDate).getData());
            case "employee-commission" -> new ReportExport("业务员业绩提成", employeeColumns(false), employeePerformance(beginDate, endDate).getData());
            case "stock-summary" -> new ReportExport("商品收发汇总表", stockColumns(false), stockSummary(beginDate, endDate).getData());
            case "inventory-change" -> new ReportExport("商品进销存变动统计", stockColumns(true), inventoryChange(beginDate, endDate).getData());
            case "bill-detail" -> new ReportExport(("PURCHASE".equals(billType) ? "进货明细" : "销售明细"), billDetailColumns(),
                    billDetail(1L, 100000L, billType, null).getData().getRecords());
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "报表类型不支持导出");
        };
    }

    private LinkedHashMap<String, String> statColumns(String amountLabel) {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("groupName", "统计维度");
        columns.put("billCount", "单据数");
        columns.put("totalQty", "数量");
        columns.put("payableAmount", amountLabel);
        columns.put("paidAmount", "已收/已付");
        columns.put("debtAmount", "欠款");
        return columns;
    }

    private LinkedHashMap<String, String> profitColumns() {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("groupName", "统计维度");
        columns.put("qty", "数量");
        columns.put("saleAmount", "销售额");
        columns.put("costAmount", "成本额");
        columns.put("profitAmount", "利润");
        return columns;
    }

    private LinkedHashMap<String, String> trendColumns() {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("date", "日期");
        columns.put("saleAmount", "销售额");
        columns.put("saleReturnAmount", "销售退货");
        columns.put("purchaseAmount", "进货额");
        columns.put("purchaseReturnAmount", "进货退货");
        return columns;
    }

    private LinkedHashMap<String, String> profitSummaryColumns() {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("saleAmount", "销售额");
        columns.put("saleReturnAmount", "销售退货");
        columns.put("purchaseAmount", "进货额");
        columns.put("purchaseReturnAmount", "进货退货");
        columns.put("otherIncome", "其他收入");
        columns.put("otherExpense", "其他支出");
        columns.put("grossProfit", "毛利");
        columns.put("netProfit", "净利润");
        return columns;
    }

    private LinkedHashMap<String, String> hotProductColumns() {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("productCode", "商品编号");
        columns.put("productName", "商品名称");
        columns.put("qty", "销售数量");
        columns.put("saleAmount", "销售额");
        columns.put("lastBillDate", "最近销售日期");
        return columns;
    }

    private LinkedHashMap<String, String> employeeColumns(boolean full) {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("employeeName", "业务员");
        if (full) {
            columns.put("saleAmount", "销售额");
            columns.put("returnAmount", "退货额");
        }
        columns.put("netAmount", "净业绩");
        columns.put("commissionAmount", "提成");
        return columns;
    }

    private LinkedHashMap<String, String> stockColumns(boolean current) {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("productName", "商品");
        columns.put("warehouseName", "仓库");
        columns.put("inQty", "入库数量");
        columns.put("outQty", "出库数量");
        columns.put("inAmount", "入库金额");
        columns.put("outAmount", "出库金额");
        columns.put("netQty", "净变动");
        if (current) {
            columns.put("currentQty", "当前库存");
        }
        return columns;
    }

    private LinkedHashMap<String, String> billDetailColumns() {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("billNo", "单号");
        columns.put("billDate", "日期");
        columns.put("partnerName", "往来单位");
        columns.put("productCode", "商品编号");
        columns.put("productName", "商品名称");
        columns.put("spec", "规格");
        columns.put("warehouseName", "仓库");
        columns.put("qty", "数量");
        columns.put("price", "单价");
        columns.put("amount", "金额");
        columns.put("discountAmount", "优惠");
        columns.put("finalAmount", "折后金额");
        return columns;
    }

    private String blankDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record ReportExport(String title, LinkedHashMap<String, String> columns, List<Map<String, Object>> rows) {
    }

    private QueryWrapper<ErpBill> billWrapper(String billType, LocalDate beginDate, LocalDate endDate) {
        return new QueryWrapper<ErpBill>()
                .eq("bill_type", billType)
                .ge(beginDate != null, "bill_date", beginDate)
                .le(endDate != null, "bill_date", endDate);
    }

    private List<Map<String, Object>> groupBills(String billType, String groupBy, LocalDate beginDate, LocalDate endDate) {
        List<ErpBill> bills = billService.list(billWrapper(billType, beginDate, endDate));
        Map<String, Map<String, Object>> rows = new HashMap<>();
        for (ErpBill bill : bills) {
            String key = groupBillKey(groupBy, bill);
            Map<String, Object> row = rows.computeIfAbsent(key, k -> baseBillStatRow(groupBy, bill));
            add(row, "billCount", BigDecimal.ONE);
            add(row, "totalQty", bill.getTotalQty());
            add(row, "totalAmount", bill.getTotalAmount());
            add(row, "discountAmount", bill.getDiscountAmount());
            add(row, "payableAmount", bill.getPayableAmount());
            add(row, "paidAmount", bill.getPaidAmount());
            add(row, "debtAmount", bill.getDebtAmount());
        }
        return sortAmount(rows.values().stream().toList(), "payableAmount");
    }

    private String groupBillKey(String groupBy, ErpBill bill) {
        if ("date".equals(groupBy)) {
            return bill.getBillDate().format(DateTimeFormatter.ISO_DATE);
        }
        if ("customer".equals(groupBy) || "supplier".equals(groupBy)) {
            return String.valueOf(bill.getPartnerId());
        }
        if ("employee".equals(groupBy)) {
            return employeeKey(bill);
        }
        return bill.getBillType();
    }

    private Map<String, Object> baseBillStatRow(String groupBy, ErpBill bill) {
        Map<String, Object> row = new HashMap<>();
        row.put("groupKey", groupBillKey(groupBy, bill));
        row.put("groupName", groupBillName(groupBy, bill));
        row.put("billCount", BigDecimal.ZERO);
        row.put("totalQty", BigDecimal.ZERO);
        row.put("totalAmount", BigDecimal.ZERO);
        row.put("discountAmount", BigDecimal.ZERO);
        row.put("payableAmount", BigDecimal.ZERO);
        row.put("paidAmount", BigDecimal.ZERO);
        row.put("debtAmount", BigDecimal.ZERO);
        return row;
    }

    private String groupBillName(String groupBy, ErpBill bill) {
        if ("date".equals(groupBy)) {
            return bill.getBillDate().format(DateTimeFormatter.ISO_DATE);
        }
        if ("customer".equals(groupBy) || "supplier".equals(groupBy)) {
            return partnerName(bill.getPartnerType(), bill.getPartnerId());
        }
        if ("employee".equals(groupBy)) {
            return employeeDisplayName(bill);
        }
        return bill.getBillType();
    }

    private String employeeKey(ErpBill bill) {
        if (bill.getEmployeeId() != null) {
            return "ID:" + bill.getEmployeeId();
        }
        String name = StrUtil.trim(bill.getEmployeeName());
        return StrUtil.isBlank(name) ? "NONE" : "NAME:" + name;
    }

    private String employeeDisplayName(ErpBill bill) {
        if (bill == null) {
            return "未指定业务员";
        }
        String name = StrUtil.trim(bill.getEmployeeName());
        if (StrUtil.isNotBlank(name)) {
            return name;
        }
        String userName = userDisplayName(bill.getEmployeeId());
        return StrUtil.isBlank(userName) ? "未指定业务员" : userName;
    }

    private String preferEmployeeName(String current, String candidate) {
        if (StrUtil.isBlank(candidate) || "未指定业务员".equals(candidate)) {
            return StrUtil.isBlank(current) ? "未指定业务员" : current;
        }
        if (StrUtil.isBlank(current) || "未指定业务员".equals(current) || current.matches("\\d+")) {
            return candidate;
        }
        return current;
    }

    private Map<String, Object> employeeSaleDetailRow(ErpBill bill, Map<String, BigDecimal> costAmounts) {
        BigDecimal costAmount = costAmounts.getOrDefault(billCostKey(bill), BigDecimal.ZERO);
        BigDecimal paidAmount = amount(bill.getPaidAmount());
        Map<String, Object> row = new HashMap<>();
        row.put("id", bill.getId());
        row.put("billNo", bill.getBillNo());
        row.put("billDate", bill.getBillDate());
        row.put("partnerName", partnerName(bill.getPartnerType(), bill.getPartnerId()));
        row.put("warehouseName", masterName(warehouseService.getById(bill.getWarehouseId())));
        row.put("paymentMethod", bill.getPaymentMethod());
        row.put("orderAmount", amount(bill.getPayableAmount()));
        row.put("saleAmount", paidAmount);
        row.put("paidAmount", paidAmount);
        row.put("costAmount", costAmount);
        row.put("profitAmount", paidAmount.subtract(costAmount));
        return row;
    }

    private String employeeDetailName(Long employeeId, String employeeName, List<ErpBill> bills) {
        String userName = userDisplayName(employeeId);
        if (StrUtil.isNotBlank(userName)) {
            return userName;
        }
        if (StrUtil.isNotBlank(employeeName)) {
            return employeeName;
        }
        return bills.stream().findFirst().map(this::employeeDisplayName).orElse("未指定业务员");
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

    private String profitKey(String groupBy, ErpBill bill, ErpBillItem item) {
        if ("bill".equals(groupBy)) {
            return String.valueOf(bill.getId());
        }
        if ("customer".equals(groupBy)) {
            return String.valueOf(bill.getPartnerId());
        }
        return String.valueOf(item.getProductId());
    }

    private Map<String, Object> baseProfitRow(String groupBy, ErpBill bill, ErpBillItem item) {
        Map<String, Object> row = new HashMap<>();
        row.put("groupKey", profitKey(groupBy, bill, item));
        row.put("groupName", switch (Objects.toString(groupBy, "product")) {
            case "bill" -> bill.getBillNo();
            case "customer" -> partnerName(bill.getPartnerType(), bill.getPartnerId());
            default -> item.getProductName();
        });
        row.put("qty", BigDecimal.ZERO);
        row.put("saleAmount", BigDecimal.ZERO);
        row.put("costAmount", BigDecimal.ZERO);
        row.put("profitAmount", BigDecimal.ZERO);
        return row;
    }

    private Map<String, Object> productRow(ErpBillItem item) {
        Map<String, Object> row = new HashMap<>();
        row.put("productId", item.getProductId());
        row.put("productCode", item.getProductCode());
        row.put("productName", item.getProductName());
        row.put("qty", BigDecimal.ZERO);
        row.put("saleAmount", BigDecimal.ZERO);
        return row;
    }

    private BigDecimal billTotal(String billType, LocalDate beginDate, LocalDate endDate) {
        return billService.list(billWrapper(billType, beginDate, endDate)).stream()
                .map(ErpBill::getPayableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal financeTotal(String billType, LocalDate beginDate, LocalDate endDate) {
        return fundFlowService.list(new QueryWrapper<ErpFundFlow>()
                        .eq("source_bill_type", billType)
                        .ge(beginDate != null, "operate_time", beginDate == null ? null : beginDate.atStartOfDay())
                        .lt(endDate != null, "operate_time", endDate == null ? null : endDate.plusDays(1).atStartOfDay()))
                .stream().map(ErpFundFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> billCostAmounts(List<ErpBill> bills) {
        if (bills.isEmpty()) {
            return Map.of();
        }
        List<Long> billIds = bills.stream().map(ErpBill::getId).filter(Objects::nonNull).toList();
        if (billIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> billTypes = bills.stream()
                .filter(bill -> bill.getId() != null)
                .collect(Collectors.toMap(ErpBill::getId, ErpBill::getBillType, (a, b) -> a));
        List<ErpBillItem> items = billItemService.list(new QueryWrapper<ErpBillItem>().in("bill_id", billIds));
        if (items.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> productCosts = productCostMap(items);
        Map<Long, BigDecimal> optionExtras = optionExtraAmountMap(items);
        Map<String, BigDecimal> result = new HashMap<>();
        for (ErpBillItem item : items) {
            result.merge(
                    billCostKey(billTypes.get(item.getBillId()), item.getBillId()),
                    itemCostAmount(item, productCosts, optionExtras),
                    BigDecimal::add);
        }
        return result;
    }

    private Map<Long, BigDecimal> productCostMap(List<ErpBillItem> items) {
        List<Long> productIds = items.stream().map(ErpBillItem::getProductId).filter(Objects::nonNull).distinct().toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(ErpProduct::getId, product -> amount(product.getPurchasePrice()), (a, b) -> a));
    }

    private Map<Long, BigDecimal> optionExtraAmountMap(List<ErpBillItem> items) {
        List<Long> optionIds = items.stream()
                .flatMap(item -> optionAttributeIds(item.getOptionAttributeIds()).stream())
                .distinct()
                .toList();
        if (optionIds.isEmpty()) {
            return Map.of();
        }
        return attributeService.listByIds(optionIds).stream()
                .collect(Collectors.toMap(ErpProductAttribute::getId, item -> amount(item.getExtraAmount()), (a, b) -> a));
    }

    private BigDecimal itemCostAmount(ErpBillItem item, Map<Long, BigDecimal> productCosts, Map<Long, BigDecimal> optionExtras) {
        BigDecimal unitCost = productCosts.getOrDefault(item.getProductId(), BigDecimal.ZERO).add(itemOptionExtraAmount(item, optionExtras));
        return amount(item.getQty()).multiply(unitCost);
    }

    private BigDecimal itemOptionExtraAmount(ErpBillItem item, Map<Long, BigDecimal> optionExtras) {
        return optionAttributeIds(item.getOptionAttributeIds()).stream()
                .map(id -> optionExtras.getOrDefault(id, BigDecimal.ZERO))
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

    private String billCostKey(ErpBill bill) {
        return billCostKey(bill.getBillType(), bill.getId());
    }

    private String billCostKey(String billType, Long billId) {
        return Objects.toString(billType, "") + ":" + Objects.toString(billId, "");
    }

    private void add(Map<String, Object> row, String key, BigDecimal amount) {
        row.put(key, amount(row.get(key)).add(amount == null ? BigDecimal.ZERO : amount));
    }

    private List<Map<String, Object>> sortAmount(List<Map<String, Object>> rows, String key) {
        return rows.stream().sorted((a, b) -> amount(b.get(key)).compareTo(amount(a.get(key)))).toList();
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
