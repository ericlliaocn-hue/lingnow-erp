package cc.lingnow.admin.controller;

import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 通用首页用户看板。
 */
@Tag(name = "数据看板")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpBillService billService;
    @Resource
    private ErpStockBalanceService stockBalanceService;
    @Resource
    private ErpPartnerFlowService partnerFlowService;
    @Resource
    private ErpFundFlowService fundFlowService;
    @Resource
    private ErpAccountService accountService;

    @Operation(summary = "获取用户维度数据看板")
    @GetMapping("/user")
    public Result<Map<String, Object>> getUserDashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long totalUsers = sysUserService.count();
        long disabledUsers = sysUserService.count(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getStatus, CommonConstants.STATUS_DISABLED));
        long activeUsers = Math.max(totalUsers - disabledUsers, 0);
        List<Map<String, Object>> onlineUsers = getOnlineUsers();
        long onlineCount = onlineUsers.size();
        long todayNewUsers = countCreatedUsers(todayStart, todayStart.plusDays(1));
        long weekNewUsers = countCreatedUsers(weekStart, LocalDateTime.now().plusSeconds(1));
        long monthNewUsers = countCreatedUsers(monthStart, LocalDateTime.now().plusSeconds(1));

        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("activeUsers", activeUsers);
        data.put("disabledUsers", disabledUsers);
        data.put("onlineCount", onlineCount);
        data.put("todayNewUsers", todayNewUsers);
        data.put("weekNewUsers", weekNewUsers);
        data.put("monthNewUsers", monthNewUsers);
        data.put("onlineRate", percent(onlineCount, activeUsers));
        data.put("disabledRate", percent(disabledUsers, totalUsers));
        data.put("onlineUsers", onlineUsers);
        data.put("userStatusStats", List.of(
                item("正常用户", activeUsers),
                item("在线用户", onlineCount),
                item("今日新增", todayNewUsers),
                item("禁用用户", disabledUsers)
        ));
        data.put("genderStats", buildGenderStats());
        data.put("growthTrend", buildGrowthTrend());
        data.put("erpStats", buildErpStats());
        return Result.success(data);
    }

    private Map<String, Object> buildErpStats() {
        LocalDate today = LocalDate.now();
        Map<String, Object> data = new HashMap<>();
        data.put("productCount", productService.count());
        data.put("customerCount", customerService.count());
        data.put("supplierCount", supplierService.count());
        data.put("todaySaleAmount", billAmount("SALE", today));
        data.put("todayPurchaseAmount", billAmount("PURCHASE", today));
        data.put("stockAmount", stockBalanceService.list().stream().map(ErpStockBalance::getCostAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        data.put("receivable", partnerTotal("RECEIVABLE").subtract(partnerTotal("RECEIVE")));
        data.put("payable", partnerTotal("PAYABLE").subtract(partnerTotal("PAY")));
        data.put("accountBalance", accountTotal());
        return data;
    }

    private java.math.BigDecimal billAmount(String billType, LocalDate date) {
        return billService.list(Wrappers.<ErpBill>lambdaQuery()
                .eq(ErpBill::getBillType, billType)
                .eq(ErpBill::getBillDate, date)
                .eq(ErpBill::getAuditStatus, 1))
                .stream().map(ErpBill::getPayableAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    private java.math.BigDecimal partnerTotal(String direction) {
        return partnerFlowService.list(Wrappers.<ErpPartnerFlow>lambdaQuery().eq(ErpPartnerFlow::getDirection, direction))
                .stream().map(ErpPartnerFlow::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    private java.math.BigDecimal accountTotal() {
        java.math.BigDecimal balance = accountService.list().stream()
                .map(ErpAccount::getOpeningBalance)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        for (ErpFundFlow flow : fundFlowService.list()) {
            balance = "IN".equals(flow.getDirection()) ? balance.add(flow.getAmount()) : balance.subtract(flow.getAmount());
        }
        return balance;
    }

    private List<Map<String, Object>> buildGrowthTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            trend.add(Map.of(
                    "date", day.toString(),
                    "value", countCreatedUsers(day.atStartOfDay(), day.plusDays(1).atStartOfDay())
            ));
        }
        return trend;
    }

    private List<Map<String, Object>> buildGenderStats() {
        return List.of(
                item("女", sysUserService.count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getGender, 0))),
                item("男", sysUserService.count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getGender, 1))),
                item("其他", sysUserService.count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getGender, 2)))
        );
    }

    private long countCreatedUsers(LocalDateTime start, LocalDateTime end) {
        return sysUserService.count(Wrappers.<SysUser>lambdaQuery()
                .ge(SysUser::getCreateTime, start)
                .lt(SysUser::getCreateTime, end));
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(value * 10000.0 / total) / 100.0;
    }

    private Map<String, Object> item(String name, Object value) {
        return Map.of("name", name, "value", value == null ? 0 : value);
    }

    private List<Map<String, Object>> getOnlineUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        for (String token : getOnlineTokens(getCurrentToken())) {
            Long userId = parseLong(getLoginId(token));
            if (userId == null) {
                continue;
            }
            SysUser sysUser = sysUserService.getById(userId);
            if (sysUser == null) {
                continue;
            }
            Map<String, Object> user = new HashMap<>();
            user.put("userId", userId);
            user.put("username", sysUser.getUsername());
            user.put("nickname", sysUser.getNickname());
            user.put("status", sysUser.getStatus());
            users.add(user);
        }
        return users;
    }

    private Set<String> getOnlineTokens(String currentToken) {
        Set<String> tokens = new LinkedHashSet<>();
        try {
            tokens.addAll(StpAdminUtil.searchTokenValue("", 0, 100, true));
        } catch (Exception e) {
            // ignore redis token search error
        }
        if (StrUtil.isNotBlank(currentToken)) {
            tokens.add(currentToken);
        }
        return tokens;
    }

    private String getCurrentToken() {
        try {
            return StpAdminUtil.getTokenValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Object getLoginId(String token) {
        try {
            return StpAdminUtil.getLoginIdByToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
