package cc.lingnow.app.controller;

import cc.lingnow.biz.erp.entity.ErpBill;
import cc.lingnow.biz.erp.entity.ErpCustomer;
import cc.lingnow.biz.erp.entity.ErpPartnerFlow;
import cc.lingnow.biz.erp.entity.ErpProduct;
import cc.lingnow.biz.erp.entity.ErpStockBalance;
import cc.lingnow.biz.erp.entity.ErpSupplier;
import cc.lingnow.biz.erp.entity.ErpUnit;
import cc.lingnow.biz.erp.service.ErpBillService;
import cc.lingnow.biz.erp.service.ErpCustomerService;
import cc.lingnow.biz.erp.service.ErpPartnerFlowService;
import cc.lingnow.biz.erp.service.ErpProductService;
import cc.lingnow.biz.erp.service.ErpStockBalanceService;
import cc.lingnow.biz.erp.service.ErpSupplierService;
import cc.lingnow.biz.erp.service.ErpUnitService;
import cc.lingnow.common.vo.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/app/erp")
@RequiredArgsConstructor
public class AppErpController {

    private final ErpCustomerService customerService;
    private final ErpSupplierService supplierService;
    private final ErpProductService productService;
    private final ErpUnitService unitService;
    private final ErpBillService billService;
    private final ErpStockBalanceService stockBalanceService;
    private final ErpPartnerFlowService partnerFlowService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerCount", customerService.count());
        data.put("supplierCount", supplierService.count());
        data.put("productCount", productService.count());
        data.put("billCount", billService.count());
        data.put("stockQty", stockBalanceService.list().stream()
                .map(ErpStockBalance::getQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        data.put("receivableAmount", partnerTotal("RECEIVABLE").subtract(partnerTotal("RECEIVE")));
        data.put("payableAmount", partnerTotal("PAYABLE").subtract(partnerTotal("PAY")));
        return Result.success(data);
    }

    @GetMapping("/customers")
    public Result<List<Map<String, Object>>> customers(@RequestParam(required = false) String keyword) {
        QueryWrapper<ErpCustomer> wrapper = new QueryWrapper<ErpCustomer>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like("name", keyword)
                        .or()
                        .like("contact", keyword)
                        .or()
                        .like("phone", keyword))
                .orderByAsc("sort_order")
                .orderByDesc("create_time");
        return Result.success(customerService.list(wrapper).stream().map(this::customerRow).toList());
    }

    @GetMapping("/suppliers")
    public Result<List<Map<String, Object>>> suppliers(@RequestParam(required = false) String keyword) {
        QueryWrapper<ErpSupplier> wrapper = new QueryWrapper<ErpSupplier>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like("name", keyword)
                        .or()
                        .like("contact", keyword)
                        .or()
                        .like("phone", keyword))
                .orderByAsc("sort_order")
                .orderByDesc("create_time");
        return Result.success(supplierService.list(wrapper).stream().map(this::supplierRow).toList());
    }

    @GetMapping("/products")
    public Result<List<Map<String, Object>>> products(@RequestParam(required = false) String keyword) {
        QueryWrapper<ErpProduct> wrapper = new QueryWrapper<ErpProduct>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like("name", keyword)
                        .or()
                        .like("code", keyword)
                        .or()
                        .like("barcode", keyword))
                .orderByAsc("sort_order")
                .orderByDesc("create_time");
        Map<Long, ErpUnit> unitMap = unitService.list().stream()
                .collect(Collectors.toMap(ErpUnit::getId, Function.identity(), (oldValue, newValue) -> oldValue));
        return Result.success(productService.list(wrapper).stream()
                .map(product -> productRow(product, unitMap.get(product.getUnitId())))
                .toList());
    }

    @GetMapping("/products/{id}")
    public Result<Map<String, Object>> productDetail(@PathVariable Long id) {
        ErpProduct product = productService.getById(id);
        if (product == null) {
            return Result.success(null);
        }
        ErpUnit unit = product.getUnitId() == null ? null : unitService.getById(product.getUnitId());
        return Result.success(productRow(product, unit));
    }

    @GetMapping("/bills")
    public Result<List<Map<String, Object>>> bills(@RequestParam(required = false) String billType) {
        QueryWrapper<ErpBill> wrapper = new QueryWrapper<ErpBill>()
                .eq(billType != null && !billType.isBlank(), "bill_type", billType)
                .orderByDesc("bill_date")
                .orderByDesc("create_time")
                .last("LIMIT 50");
        return Result.success(billService.list(wrapper).stream().map(this::billRow).toList());
    }

    private Map<String, Object> customerRow(ErpCustomer customer) {
        Map<String, Object> row = masterRow(customer.getId(), customer.getCode(), customer.getName(),
                customer.getContact(), customer.getPhone(), customer.getAddress(), customer.getStatus(), customer.getRemark());
        row.put("partnerType", "CUSTOMER");
        row.put("openingBalance", customer.getOpeningBalance());
        row.put("discountRate", customer.getDiscountRate());
        return row;
    }

    private Map<String, Object> supplierRow(ErpSupplier supplier) {
        Map<String, Object> row = masterRow(supplier.getId(), supplier.getCode(), supplier.getName(),
                supplier.getContact(), supplier.getPhone(), supplier.getAddress(), supplier.getStatus(), supplier.getRemark());
        row.put("partnerType", "SUPPLIER");
        row.put("openingBalance", supplier.getOpeningBalance());
        return row;
    }

    private Map<String, Object> masterRow(Long id, String code, String name, String contact, String phone,
                                          String address, Integer status, String remark) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("code", code);
        row.put("name", name);
        row.put("contact", contact);
        row.put("phone", phone);
        row.put("address", address);
        row.put("status", status);
        row.put("remark", remark);
        return row;
    }

    private Map<String, Object> productRow(ErpProduct product, ErpUnit unit) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", product.getId());
        row.put("code", product.getCode());
        row.put("name", product.getName());
        row.put("spec", product.getSpec());
        row.put("unitId", product.getUnitId());
        row.put("unitName", unit == null ? null : unit.getName());
        row.put("barcode", product.getBarcode());
        row.put("imageUrl", product.getImageUrl());
        row.put("salePrice", product.getSalePrice());
        row.put("retailPrice", product.getRetailPrice());
        row.put("purchasePrice", product.getPurchasePrice());
        row.put("stockQty", stockQty(product.getId()));
        row.put("status", product.getStatus());
        row.put("remark", product.getRemark());
        return row;
    }

    private Map<String, Object> billRow(ErpBill bill) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", bill.getId());
        row.put("billNo", bill.getBillNo());
        row.put("billType", bill.getBillType());
        row.put("billDate", bill.getBillDate());
        row.put("partnerId", bill.getPartnerId());
        row.put("partnerType", bill.getPartnerType());
        row.put("partnerName", partnerName(bill.getPartnerType(), bill.getPartnerId()));
        row.put("totalQty", bill.getTotalQty());
        row.put("payableAmount", bill.getPayableAmount());
        row.put("paidAmount", bill.getPaidAmount());
        row.put("debtAmount", bill.getDebtAmount());
        row.put("auditStatus", bill.getAuditStatus());
        row.put("paymentStatus", bill.getPaymentStatus());
        row.put("remark", bill.getRemark());
        return row;
    }

    private BigDecimal stockQty(Long productId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        return stockBalanceService.list(new QueryWrapper<ErpStockBalance>().eq("product_id", productId))
                .stream()
                .map(ErpStockBalance::getQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal partnerTotal(String direction) {
        return partnerFlowService.list(new QueryWrapper<ErpPartnerFlow>().eq("direction", direction))
                .stream()
                .map(ErpPartnerFlow::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String partnerName(String partnerType, Long partnerId) {
        if (partnerId == null || partnerType == null) {
            return null;
        }
        if ("CUSTOMER".equals(partnerType)) {
            ErpCustomer customer = customerService.getById(partId(partnerId));
            return customer == null ? null : customer.getName();
        }
        if ("SUPPLIER".equals(partnerType)) {
            ErpSupplier supplier = supplierService.getById(partId(partnerId));
            return supplier == null ? null : supplier.getName();
        }
        return null;
    }

    private Long partId(Long id) {
        return id;
    }
}
