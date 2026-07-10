package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpCustomerAccountQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpCustomerAccountSaveBO;
import cc.lingnow.admin.model.vo.erp.ErpCustomerAccountVO;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.ErpCustomer;
import cc.lingnow.biz.erp.entity.ErpCustomerAccount;
import cc.lingnow.biz.erp.service.ErpCustomerAccountService;
import cc.lingnow.biz.erp.service.ErpCustomerService;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/erp/customer-account")
@RequiredArgsConstructor
public class ErpCustomerAccountController {

    private final ErpCustomerAccountService accountService;
    private final ErpCustomerService customerService;

    @GetMapping("/list")
    public Result<PageResult<ErpCustomerAccountVO>> list(ErpCustomerAccountQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-account:list");
        QueryWrapper<ErpCustomerAccount> wrapper = new QueryWrapper<ErpCustomerAccount>()
                .like(StrUtil.isNotBlank(query.getUsername()), "username", query.getUsername())
                .eq(query.getStatus() != null, "status", query.getStatus())
                .orderByDesc("create_time");
        if (StrUtil.isNotBlank(query.getCustomerName())) {
            List<Long> customerIds = customerService.list(new QueryWrapper<ErpCustomer>()
                            .like("name", query.getCustomerName()))
                    .stream()
                    .map(ErpCustomer::getId)
                    .toList();
            if (customerIds.isEmpty()) {
                return Result.success(PageResult.of(query.getCurrent(), query.getSize(), 0L, List.of()));
            }
            wrapper.in("customer_id", customerIds);
        }
        Page<ErpCustomerAccount> page = accountService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ErpCustomerAccountVO> records = page.getRecords().stream().map(this::toVO).toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @PostMapping
    @Log(title = "客户账号", businessType = BusinessType.INSERT)
    public Result<Void> add(@Valid @RequestBody ErpCustomerAccountSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-account:add");
        requireCustomer(bo.getCustomerId());
        if (StrUtil.isBlank(bo.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "初始密码不能为空");
        }
        ensureUsernameUnique(bo.getUsername(), null);
        ErpCustomerAccount account = BeanUtil.copyProperties(bo, ErpCustomerAccount.class);
        account.setPassword(BCrypt.hashpw(bo.getPassword()));
        account.setStatus(bo.getStatus() == null ? CommonConstants.STATUS_NORMAL : bo.getStatus());
        accountService.save(account);
        return Result.success();
    }

    @PutMapping
    @Log(title = "客户账号", businessType = BusinessType.UPDATE)
    public Result<Void> edit(@Valid @RequestBody ErpCustomerAccountSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-account:edit");
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        ErpCustomerAccount old = accountService.getById(bo.getId());
        if (old == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        requireCustomer(bo.getCustomerId());
        ensureUsernameUnique(bo.getUsername(), bo.getId());
        ErpCustomerAccount account = BeanUtil.copyProperties(bo, ErpCustomerAccount.class);
        if (StrUtil.isBlank(bo.getPassword())) {
            account.setPassword(old.getPassword());
        } else {
            account.setPassword(BCrypt.hashpw(bo.getPassword()));
        }
        account.setStatus(bo.getStatus() == null ? CommonConstants.STATUS_NORMAL : bo.getStatus());
        accountService.updateById(account);
        return Result.success();
    }

    @PutMapping("/{id}/status/{status}")
    @Log(title = "客户账号状态", businessType = BusinessType.UPDATE)
    public Result<Void> status(@PathVariable Long id, @PathVariable Integer status) {
        StpAdminUtil.stpLogic.checkPermission("erp:customer-account:edit");
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        accountService.update(new UpdateWrapper<ErpCustomerAccount>().eq("id", id).set("status", status));
        return Result.success();
    }

    private void ensureUsernameUnique(String username, Long id) {
        ErpCustomerAccount exists = accountService.getOne(new QueryWrapper<ErpCustomerAccount>()
                .eq("username", username)
                .last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号已存在");
        }
    }

    private void requireCustomer(Long customerId) {
        ErpCustomer customer = customerService.getById(customerId);
        if (customer == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "客户不存在");
        }
    }

    private ErpCustomerAccountVO toVO(ErpCustomerAccount account) {
        ErpCustomerAccountVO vo = BeanUtil.copyProperties(account, ErpCustomerAccountVO.class);
        ErpCustomer customer = customerService.getById(account.getCustomerId());
        vo.setCustomerName(customer == null ? null : customer.getName());
        return vo;
    }
}
