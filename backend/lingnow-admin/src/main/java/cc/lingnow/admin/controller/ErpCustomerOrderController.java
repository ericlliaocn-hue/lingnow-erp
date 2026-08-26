package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpCustomerOrderCancelBO;
import cc.lingnow.admin.model.bo.erp.ErpCustomerOrderConfirmBO;
import cc.lingnow.admin.model.bo.erp.ErpCustomerOrderQueryBO;
import cc.lingnow.admin.model.enums.ErpApprovalStatus;
import cc.lingnow.admin.model.vo.erp.ErpCustomerOrderVO;
import cc.lingnow.admin.service.ErpAuditService;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.biz.notification.service.SysUserNotificationService;
import cc.lingnow.biz.role.service.SysRoleService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/erp/customer-order")
@RequiredArgsConstructor
public class ErpCustomerOrderController {

    private static final List<String> PAYMENT_METHODS = List.of("淘宝", "1688", "小红书", "微信", "支付宝", "银行卡");

    private final ErpCustomerOrderService orderService;
    private final ErpCustomerOrderItemService orderItemService;
    private final ErpBillService billService;
    private final ErpBillItemService billItemService;
    private final ErpProductService productService;
    private final ErpProductAttributeService attributeService;
    private final ErpBillNoRuleService billNoRuleService;
    private final ErpWarehouseService warehouseService;
    private final ErpAccountService accountService;
    private final ErpAuditService auditService;
    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysUserNotificationService notificationService;

    @GetMapping("/list")
    public Result<PageResult<ErpCustomerOrderVO>> list(ErpCustomerOrderQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-order:list");
        QueryWrapper<ErpCustomerOrder> wrapper = new QueryWrapper<ErpCustomerOrder>()
                .like(StrUtil.isNotBlank(query.getOrderNo()), "order_no", query.getOrderNo())
                .like(StrUtil.isNotBlank(query.getCustomerName()), "customer_name", query.getCustomerName())
                .eq(StrUtil.isNotBlank(query.getStatus()), "status", query.getStatus())
                .orderByDesc("create_time");
        Page<ErpCustomerOrder> page = orderService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ErpCustomerOrderVO> records = page.getRecords().stream().map(item -> toVO(item, false)).toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @GetMapping("/{id}")
    public Result<ErpCustomerOrderVO> getInfo(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-order:list");
        return Result.success(toVO(requireOrder(id), true));
    }

    @GetMapping("/print/{id}")
    public Result<ErpCustomerOrderVO> print(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-order:print");
        return Result.success(toVO(requireOrder(id), true));
    }

    @PutMapping("/{id}/cancel")
    @Log(title = "客户订单作废", businessType = BusinessType.UPDATE)
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) ErpCustomerOrderCancelBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-order:cancel");
        ErpCustomerOrder order = requireOrder(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有待接单订单可以作废");
        }
        orderService.update(new UpdateWrapper<ErpCustomerOrder>()
                .eq("id", id)
                .set("status", "CANCELLED")
                .set("cancel_time", LocalDateTime.now())
                .set("cancel_by", currentUserName())
                .set("cancel_reason", bo == null ? null : bo.getReason()));
        return Result.success();
    }

    @PostMapping("/{id}/confirm")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "客户订单确认转销售单", businessType = BusinessType.INSERT)
    public Result<Long> confirm(@PathVariable Long id, @Valid @RequestBody ErpCustomerOrderConfirmBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-order:confirm");
        ErpCustomerOrder order = requireOrder(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有待接单订单可以确认");
        }
        requireWarehouse(bo.getWarehouseId());
        BigDecimal paidAmount = nvl(bo.getPaidAmount());
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "付款金额不能小于0");
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (bo.getAccountId() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "存在付款金额时必须选择账户");
            }
            requireAccount(bo.getAccountId());
            if (!PAYMENT_METHODS.contains(bo.getPaymentMethod())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "付款方式不正确");
            }
        }

        List<ErpCustomerOrderItem> orderItems = orderItemService.list(new QueryWrapper<ErpCustomerOrderItem>().eq("order_id", id));
        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品明细不能为空");
        }
        BigDecimal payable = nvl(order.getTotalAmount());
        ErpBill bill = new ErpBill();
        bill.setBillNo(billNoRuleService.nextNo("SALE"));
        bill.setBillType("SALE");
        bill.setBillDate(LocalDate.now());
        bill.setPartnerId(order.getCustomerId());
        bill.setPartnerType("CUSTOMER");
        bill.setWarehouseId(bo.getWarehouseId());
        bill.setAccountId(bo.getAccountId());
        Long sourceEmployeeId = order.getEmployeeId() == null ? currentUserId() : order.getEmployeeId();
        bill.setEmployeeId(bo.getEmployeeId() == null ? sourceEmployeeId : bo.getEmployeeId());
        bill.setEmployeeName(StrUtil.blankToDefault(bo.getEmployeeName(),
                StrUtil.blankToDefault(order.getEmployeeName(), userDisplayName(bill.getEmployeeId()))));
        bill.setReceiverName(order.getReceiverName());
        bill.setReceiverPhone(order.getReceiverPhone());
        bill.setReceiverAddress(order.getReceiverAddress());
        bill.setTotalQty(nvl(order.getTotalQty()));
        bill.setTotalAmount(payable);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setOtherAmount(BigDecimal.ZERO);
        bill.setPayableAmount(payable);
        bill.setPaidAmount(paidAmount);
        bill.setPaymentMethod(paidAmount.compareTo(BigDecimal.ZERO) > 0 ? bo.getPaymentMethod() : null);
        bill.setDebtAmount(payable.subtract(paidAmount));
        bill.setPaymentStatus(paymentStatus(payable, paidAmount));
        bill.setAuditStatus(0);
        bill.setApprovalStatus(ErpApprovalStatus.NONE);
        bill.setRemark(StrUtil.isNotBlank(bo.getRemark()) ? bo.getRemark() : order.getRemark());
        billService.save(bill);

        List<ErpBillItem> billItems = orderItems.stream().map(item -> toBillItem(item, bill.getId(), bo.getWarehouseId())).toList();
        billItemService.saveBatch(billItems);
        auditService.auditBill(bill.getId());
        notifyNewSaleBill(bill);

        orderService.update(new UpdateWrapper<ErpCustomerOrder>()
                .eq("id", id)
                .set("status", "CONFIRMED")
                .set("bill_id", bill.getId())
                .set("bill_no", bill.getBillNo())
                .set("confirm_time", LocalDateTime.now())
                .set("confirm_by", currentUserName()));
        return Result.success(bill.getId());
    }

    private void notifyNewSaleBill(ErpBill bill) {
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

    private ErpBillItem toBillItem(ErpCustomerOrderItem source, Long billId, Long warehouseId) {
        BigDecimal attributeExtraAmount = optionExtraAmount(source.getOptionAttributeIds());
        ErpProduct product = productService.getById(source.getProductId());
        ErpBillItem item = new ErpBillItem();
        item.setBillId(billId);
        item.setProductId(source.getProductId());
        item.setProductCode(source.getProductCode());
        item.setProductName(source.getProductName());
        item.setProductImageUrl(source.getProductImageUrl());
        item.setLogoImageUrl(source.getLogoImageUrl());
        item.setSpec(source.getSpec());
        item.setAttributeText(source.getAttributeText());
        item.setCategoryLevel1Id(source.getCategoryLevel1Id());
        item.setCategoryLevel1Name(source.getCategoryLevel1Name());
        item.setCategoryLevel2Id(source.getCategoryLevel2Id());
        item.setCategoryLevel2Name(source.getCategoryLevel2Name());
        item.setOptionAttributeIds(source.getOptionAttributeIds());
        item.setOptionAttributeText(source.getOptionAttributeText());
        item.setUnitId(source.getUnitId());
        item.setWarehouseId(warehouseId);
        item.setQty(nvl(source.getQty()));
        item.setBasePrice(nvl(source.getPrice()).subtract(attributeExtraAmount));
        item.setAttributeExtraAmount(attributeExtraAmount);
        item.setCostPrice(product == null ? BigDecimal.ZERO : nvl(product.getPurchasePrice()));
        item.setPrice(nvl(source.getPrice()));
        item.setAmount(nvl(source.getAmount()));
        item.setDiscountRate(new BigDecimal("100"));
        item.setDiscountAmount(BigDecimal.ZERO);
        item.setFinalAmount(nvl(source.getAmount()));
        item.setRemark(source.getRemark());
        return item;
    }

    private BigDecimal optionExtraAmount(String value) {
        if (StrUtil.isBlank(value)) {
            return BigDecimal.ZERO;
        }
        List<Long> ids = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(this::parseLong)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return attributeService.listByIds(ids).stream()
                .map(item -> nvl(item.getExtraAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ErpCustomerOrder requireOrder(Long id) {
        ErpCustomerOrder order = orderService.getById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return order;
    }

    private void requireWarehouse(Long id) {
        ErpWarehouse warehouse = warehouseService.getById(id);
        if (warehouse == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(warehouse.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仓库不存在或已停用");
        }
    }

    private void requireAccount(Long id) {
        ErpAccount account = accountService.getById(id);
        if (account == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户不存在或已停用");
        }
    }

    private ErpCustomerOrderVO toVO(ErpCustomerOrder order, boolean withItems) {
        ErpCustomerOrderVO vo = BeanUtil.copyProperties(order, ErpCustomerOrderVO.class);
        if (withItems) {
            vo.setItems(orderItemService.list(new QueryWrapper<ErpCustomerOrderItem>().eq("order_id", order.getId()))
                    .stream()
                    .map(item -> BeanUtil.copyProperties(item, ErpCustomerOrderVO.Item.class))
                    .toList());
        }
        return vo;
    }

    private Long currentUserId() {
        Object loginId = StpAdminUtil.getLoginIdDefaultNull();
        return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
    }

    private String currentUserName() {
        Long userId = currentUserId();
        return userId == null ? null : userDisplayName(userId);
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

    private String paymentStatus(BigDecimal payable, BigDecimal paid) {
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }
        return paid.compareTo(payable) >= 0 ? "PAID" : "PARTIAL";
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }
}
