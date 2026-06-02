package cc.lingnow.admin.service.impl;

import cc.lingnow.admin.service.ErpAuditService;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ErpAuditServiceImpl implements ErpAuditService {

    private final ErpBillService billService;
    private final ErpBillItemService billItemService;
    private final ErpFinanceBillService financeBillService;
    private final ErpStockCheckService stockCheckService;
    private final ErpStockCheckItemService stockCheckItemService;
    private final ErpStockBalanceService stockBalanceService;
    private final ErpStockFlowService stockFlowService;
    private final ErpFundFlowService fundFlowService;
    private final ErpPartnerFlowService partnerFlowService;
    private final ErpAccountService accountService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditBill(Long id) {
        ErpBill bill = requireBill(id);
        ensureCanAudit(bill);
        List<ErpBillItem> items = billItemService.list(new QueryWrapper<ErpBillItem>().eq("bill_id", id));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品明细不能为空");
        }
        if ("SALE".equals(bill.getBillType())) {
            auditSale(bill, items);
        } else if ("SALE_RETURN".equals(bill.getBillType())) {
            auditSaleReturn(bill, items);
        } else if ("PURCHASE_RETURN".equals(bill.getBillType())) {
            auditPurchaseReturn(bill, items);
        } else {
            auditPurchase(bill, items);
        }
        bill.setAuditStatus(1);
        bill.setAuditTime(LocalDateTime.now());
        bill.setAuditBy(String.valueOf(StpAdminUtil.getLoginIdDefaultNull()));
        billService.updateById(bill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditBill(Long id) {
        ErpBill bill = requireBill(id);
        if (!Integer.valueOf(1).equals(bill.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据未审核");
        }
        rollbackStock(bill);
        fundFlowService.remove(new QueryWrapper<ErpFundFlow>().eq("source_bill_id", bill.getId()).eq("source_bill_type", bill.getBillType()));
        partnerFlowService.remove(new QueryWrapper<ErpPartnerFlow>().eq("source_bill_id", bill.getId()).eq("source_bill_type", bill.getBillType()));
        bill.setAuditStatus(0);
        bill.setAuditTime(null);
        bill.setAuditBy(null);
        billService.updateById(bill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditFinanceBill(Long id) {
        ErpFinanceBill bill = requireFinanceBill(id);
        if (Integer.valueOf(1).equals(bill.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据已审核，不能重复审核");
        }
        if ("RECEIPT".equals(bill.getBillType())) {
            addFundFlow(bill, "IN", "收款单");
            addPartnerFlow(bill, "RECEIVE", "客户收款");
        } else if ("PAYMENT".equals(bill.getBillType())) {
            addFundFlow(bill, "OUT", "付款单");
            addPartnerFlow(bill, "PAY", "供应商付款");
        } else if ("INCOME".equals(bill.getBillType())) {
            addFundFlow(bill, "IN", "其他收入");
        } else {
            addFundFlow(bill, "OUT", "其他支出");
        }
        bill.setAuditStatus(1);
        bill.setAuditTime(LocalDateTime.now());
        bill.setAuditBy(String.valueOf(StpAdminUtil.getLoginIdDefaultNull()));
        financeBillService.updateById(bill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditFinanceBill(Long id) {
        ErpFinanceBill bill = requireFinanceBill(id);
        if (!Integer.valueOf(1).equals(bill.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据未审核");
        }
        fundFlowService.remove(new QueryWrapper<ErpFundFlow>().eq("source_bill_id", bill.getId()).eq("source_bill_type", bill.getBillType()));
        partnerFlowService.remove(new QueryWrapper<ErpPartnerFlow>().eq("source_bill_id", bill.getId()).eq("source_bill_type", bill.getBillType()));
        bill.setAuditStatus(0);
        bill.setAuditTime(null);
        bill.setAuditBy(null);
        financeBillService.updateById(bill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditStockCheck(Long id) {
        ErpStockCheck check = requireStockCheck(id);
        if (Integer.valueOf(1).equals(check.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "盘点单已审核，不能重复审核");
        }
        List<ErpStockCheckItem> items = stockCheckItemService.list(new QueryWrapper<ErpStockCheckItem>().eq("check_id", id));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "盘点明细不能为空");
        }
        for (ErpStockCheckItem item : items) {
            changeStock(check, item);
        }
        check.setAuditStatus(1);
        check.setAuditTime(LocalDateTime.now());
        check.setAuditBy(String.valueOf(StpAdminUtil.getLoginIdDefaultNull()));
        stockCheckService.updateById(check);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditStockCheck(Long id) {
        ErpStockCheck check = requireStockCheck(id);
        if (!Integer.valueOf(1).equals(check.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "盘点单未审核");
        }
        rollbackStock(check);
        check.setAuditStatus(0);
        check.setAuditTime(null);
        check.setAuditBy(null);
        stockCheckService.updateById(check);
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
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, item.getProductName() + "库存不足");
        }
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
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "反审核后库存不能为负");
            }
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

    private void changeStock(ErpStockCheck check, ErpStockCheckItem item) {
        if (nvl(item.getDiffQty()).compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        ErpStockBalance balance = stockBalanceService.getOne(new QueryWrapper<ErpStockBalance>()
                .eq("product_id", item.getProductId()).eq("warehouse_id", item.getWarehouseId()).last("limit 1"));
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
        flow.setFlowNo(flowNo("ST"));
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
            ErpStockBalance balance = stockBalanceService.getOne(new QueryWrapper<ErpStockBalance>()
                    .eq("product_id", flow.getProductId()).eq("warehouse_id", flow.getWarehouseId()).last("limit 1"));
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
        List<ErpFundFlow> flows = fundFlowService.list(new QueryWrapper<ErpFundFlow>().eq("account_id", accountId));
        BigDecimal balance = nvl(accountService.getById(accountId) == null ? BigDecimal.ZERO : accountService.getById(accountId).getOpeningBalance());
        for (ErpFundFlow flow : flows) {
            balance = "IN".equals(flow.getDirection()) ? balance.add(flow.getAmount()) : balance.subtract(flow.getAmount());
        }
        return balance;
    }

    private ErpBill requireBill(Long id) {
        ErpBill bill = billService.getById(id);
        if (bill == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return bill;
    }

    private ErpFinanceBill requireFinanceBill(Long id) {
        ErpFinanceBill bill = financeBillService.getById(id);
        if (bill == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return bill;
    }

    private ErpStockCheck requireStockCheck(Long id) {
        ErpStockCheck check = stockCheckService.getById(id);
        if (check == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return check;
    }

    private void ensureCanAudit(ErpBill bill) {
        if (Integer.valueOf(1).equals(bill.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据已审核，不能重复审核");
        }
    }

    private String flowNo(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
