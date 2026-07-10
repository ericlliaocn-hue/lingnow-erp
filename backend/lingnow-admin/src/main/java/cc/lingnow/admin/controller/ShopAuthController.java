package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.shop.ShopLoginBO;
import cc.lingnow.admin.model.bo.shop.ShopRegisterBO;
import cc.lingnow.admin.model.vo.shop.ShopLoginVO;
import cc.lingnow.admin.util.StpShopUtil;
import cc.lingnow.biz.erp.entity.ErpCustomer;
import cc.lingnow.biz.erp.entity.ErpCustomerAccount;
import cc.lingnow.biz.erp.service.ErpCustomerAccountService;
import cc.lingnow.biz.erp.service.ErpCustomerService;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@RestController
@RequestMapping("/shop-api/auth")
@RequiredArgsConstructor
public class ShopAuthController {

    private final ErpCustomerAccountService accountService;
    private final ErpCustomerService customerService;

    private static final DateTimeFormatter CUSTOMER_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @PostMapping("/login")
    public Result<ShopLoginVO> login(@Valid @RequestBody ShopLoginBO bo) {
        ErpCustomerAccount account = accountService.getOne(new QueryWrapper<ErpCustomerAccount>()
                .eq("username", bo.getUsername())
                .last("limit 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        if (!Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!BCrypt.checkpw(bo.getPassword(), account.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        ErpCustomer customer = customerService.getById(account.getCustomerId());
        if (customer == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(customer.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "客户不存在或已停用");
        }
        StpShopUtil.login(account.getId());
        accountService.update(new UpdateWrapper<ErpCustomerAccount>()
                .eq("id", account.getId())
                .set("last_login_time", LocalDateTime.now()));
        return Result.success(toLoginVO(account, customer));
    }

    @PostMapping("/register")
    public Result<ShopLoginVO> register(@Valid @RequestBody ShopRegisterBO bo) {
        if (!Objects.equals(bo.getPassword(), bo.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        ensureRegisterPhoneUnique(bo.getPhone());

        ErpCustomer customer = new ErpCustomer();
        customer.setCode(nextCustomerCode());
        customer.setName(bo.getName());
        customer.setContact(bo.getName());
        customer.setPhone(bo.getPhone());
        customer.setParentId(0L);
        customer.setStatus(CommonConstants.STATUS_NORMAL);
        customer.setSortOrder(0);
        customer.setRemark("客户自助注册");
        customerService.save(customer);

        ErpCustomerAccount account = new ErpCustomerAccount();
        account.setCustomerId(customer.getId());
        account.setUsername(bo.getPhone());
        account.setPassword(BCrypt.hashpw(bo.getPassword()));
        account.setNickname(bo.getName());
        account.setPhone(bo.getPhone());
        account.setStatus(CommonConstants.STATUS_NORMAL);
        account.setLastLoginTime(LocalDateTime.now());
        account.setRemark("客户自助注册");
        accountService.save(account);

        StpShopUtil.login(account.getId());
        return Result.success(toLoginVO(account, customer));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpShopUtil.isLogin()) {
            StpShopUtil.logout();
        }
        return Result.success();
    }

    @GetMapping("/me")
    public Result<ShopLoginVO> me() {
        ErpCustomerAccount account = currentAccount();
        ErpCustomer customer = customerService.getById(account.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return Result.success(toLoginVO(account, customer));
    }

    private ErpCustomerAccount currentAccount() {
        StpShopUtil.checkLogin();
        ErpCustomerAccount account = accountService.getById(StpShopUtil.getLoginIdAsLong());
        if (account == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return account;
    }

    private void ensureRegisterPhoneUnique(String phone) {
        ErpCustomerAccount exists = accountService.getOne(new QueryWrapper<ErpCustomerAccount>()
                .eq("username", phone)
                .last("limit 1"));
        if (exists != null) {
            throw new BusinessException(ErrorCode.USER_EXIST.getCode(), "手机号已注册，请直接登录");
        }
    }

    private String nextCustomerCode() {
        String code;
        int index = 0;
        do {
            code = "KH" + LocalDateTime.now().format(CUSTOMER_CODE_FORMATTER) + (index == 0 ? "" : String.valueOf(index));
            index++;
        } while (customerService.getOne(new QueryWrapper<ErpCustomer>().eq("code", code).last("limit 1")) != null);
        return code;
    }

    private ShopLoginVO toLoginVO(ErpCustomerAccount account, ErpCustomer customer) {
        ShopLoginVO vo = new ShopLoginVO();
        vo.setToken(StpShopUtil.getTokenValue());
        vo.setAccountId(account.getId());
        vo.setCustomerId(customer.getId());
        vo.setUsername(account.getUsername());
        vo.setNickname(StrUtil.blankToDefault(account.getNickname(), account.getUsername()));
        vo.setCustomerName(customer.getName());
        return vo;
    }
}
