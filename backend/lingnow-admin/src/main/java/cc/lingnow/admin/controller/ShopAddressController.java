package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.shop.ShopAddressSaveBO;
import cc.lingnow.admin.model.vo.shop.ShopAddressVO;
import cc.lingnow.admin.util.StpShopUtil;
import cc.lingnow.biz.erp.entity.ErpCustomer;
import cc.lingnow.biz.erp.entity.ErpCustomerAccount;
import cc.lingnow.biz.erp.entity.ErpCustomerAddress;
import cc.lingnow.biz.erp.model.ErpAddressParseBO;
import cc.lingnow.biz.erp.model.ErpAddressParseVO;
import cc.lingnow.biz.erp.service.ErpAddressParseService;
import cc.lingnow.biz.erp.service.ErpCustomerAccountService;
import cc.lingnow.biz.erp.service.ErpCustomerAddressService;
import cc.lingnow.biz.erp.service.ErpCustomerService;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/shop-api")
@RequiredArgsConstructor
public class ShopAddressController {

    private final ErpCustomerAccountService accountService;
    private final ErpCustomerService customerService;
    private final ErpCustomerAddressService addressService;
    private final ErpAddressParseService addressParseService;

    @GetMapping("/addresses")
    public Result<List<ShopAddressVO>> list(@RequestParam(required = false) String keyword) {
        ErpCustomerAccount account = currentAccount();
        QueryWrapper<ErpCustomerAddress> wrapper = new QueryWrapper<ErpCustomerAddress>()
                .eq("account_id", account.getId())
                .orderByDesc("default_flag")
                .orderByDesc("update_time")
                .orderByDesc("create_time");
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(item -> item.like("receiver_name", keyword)
                    .or().like("receiver_phone", keyword)
                    .or().like("province_name", keyword)
                    .or().like("city_name", keyword)
                    .or().like("district_name", keyword)
                    .or().like("street_name", keyword)
                    .or().like("village_name", keyword)
                    .or().like("detail_address", keyword)
                    .or().like("full_address", keyword));
        }
        return Result.success(addressService.list(wrapper).stream().map(this::toVO).toList());
    }

    @GetMapping("/addresses/{id}")
    public Result<ShopAddressVO> detail(@PathVariable Long id) {
        ErpCustomerAccount account = currentAccount();
        return Result.success(toVO(requireOwnedAddress(id, account)));
    }

    @PostMapping("/addresses")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> create(@Valid @RequestBody ShopAddressSaveBO bo) {
        ErpCustomerAccount account = currentAccount();
        boolean firstAddress = addressService.count(new QueryWrapper<ErpCustomerAddress>().eq("account_id", account.getId())) == 0;
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(bo.getDefaultFlag());
        if (makeDefault) {
            clearDefault(account.getId());
        }
        ErpCustomerAddress entity = new ErpCustomerAddress();
        entity.setCustomerId(account.getCustomerId());
        entity.setAccountId(account.getId());
        applyAddress(entity, bo);
        entity.setDefaultFlag(makeDefault ? 1 : 0);
        addressService.save(entity);
        return Result.success(toVO(entity));
    }

    @PutMapping("/addresses/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> update(@PathVariable Long id, @Valid @RequestBody ShopAddressSaveBO bo) {
        ErpCustomerAccount account = currentAccount();
        ErpCustomerAddress entity = requireOwnedAddress(id, account);
        boolean makeDefault = Boolean.TRUE.equals(bo.getDefaultFlag());
        if (makeDefault) {
            clearDefault(account.getId());
        }
        applyAddress(entity, bo);
        entity.setDefaultFlag(makeDefault ? 1 : 0);
        addressService.updateById(entity);
        ensureOneDefault(account.getId(), entity.getId());
        return Result.success(toVO(addressService.getById(id)));
    }

    @PostMapping("/addresses/{id}/default")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> setDefault(@PathVariable Long id) {
        ErpCustomerAccount account = currentAccount();
        ErpCustomerAddress entity = requireOwnedAddress(id, account);
        clearDefault(account.getId());
        entity.setDefaultFlag(1);
        addressService.updateById(entity);
        return Result.success(toVO(entity));
    }

    @PostMapping("/address/parse")
    public Result<ErpAddressParseVO> parseAddress(@Valid @RequestBody ErpAddressParseBO bo) {
        currentAccount();
        return Result.success(addressParseService.parse(bo.getRawText()));
    }

    private ErpCustomerAccount currentAccount() {
        StpShopUtil.checkLogin();
        ErpCustomerAccount account = accountService.getById(StpShopUtil.getLoginIdAsLong());
        if (account == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        ErpCustomer customer = customerService.getById(account.getCustomerId());
        if (customer == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(customer.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "客户不存在或已停用");
        }
        return account;
    }

    private ErpCustomerAddress requireOwnedAddress(Long id, ErpCustomerAccount account) {
        ErpCustomerAddress address = addressService.getById(id);
        if (address == null || !Objects.equals(address.getAccountId(), account.getId())) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return address;
    }

    private void applyAddress(ErpCustomerAddress entity, ShopAddressSaveBO bo) {
        List<String> path = normalizeList(bo.getRegionPath());
        List<String> names = normalizeList(bo.getRegionPathNames());
        entity.setReceiverName(StrUtil.trim(bo.getReceiverName()));
        entity.setReceiverPhone(StrUtil.trim(bo.getReceiverPhone()));
        entity.setProvinceCode(valueAt(path, 0));
        entity.setProvinceName(valueAt(names, 0));
        entity.setCityCode(valueAt(path, 1));
        entity.setCityName(valueAt(names, 1));
        entity.setDistrictCode(valueAt(path, 2));
        entity.setDistrictName(valueAt(names, 2));
        entity.setStreetCode(valueAt(path, 3));
        entity.setStreetName(valueAt(names, 3));
        entity.setVillageCode(valueAt(path, 4));
        entity.setVillageName(valueAt(names, 4));
        entity.setDetailAddress(StrUtil.trim(bo.getDetailAddress()));
        entity.setAddressLabel(StrUtil.trim(bo.getAddressLabel()));
        entity.setFullAddress(buildFullAddress(names, bo.getDetailAddress()));
    }

    private void clearDefault(Long accountId) {
        addressService.update(new UpdateWrapper<ErpCustomerAddress>()
                .eq("account_id", accountId)
                .set("default_flag", 0));
    }

    private void ensureOneDefault(Long accountId, Long fallbackId) {
        long defaultCount = addressService.count(new QueryWrapper<ErpCustomerAddress>()
                .eq("account_id", accountId)
                .eq("default_flag", 1));
        if (defaultCount > 0) {
            return;
        }
        addressService.update(new UpdateWrapper<ErpCustomerAddress>()
                .eq("id", fallbackId)
                .eq("account_id", accountId)
                .set("default_flag", 1));
    }

    private ShopAddressVO toVO(ErpCustomerAddress entity) {
        ShopAddressVO vo = new ShopAddressVO();
        vo.setId(entity.getId());
        vo.setReceiverName(entity.getReceiverName());
        vo.setReceiverPhone(entity.getReceiverPhone());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setProvinceName(entity.getProvinceName());
        vo.setCityCode(entity.getCityCode());
        vo.setCityName(entity.getCityName());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setDistrictName(entity.getDistrictName());
        vo.setStreetCode(entity.getStreetCode());
        vo.setStreetName(entity.getStreetName());
        vo.setVillageCode(entity.getVillageCode());
        vo.setVillageName(entity.getVillageName());
        vo.setRegionPath(nonBlankList(entity.getProvinceCode(), entity.getCityCode(), entity.getDistrictCode(), entity.getStreetCode(), entity.getVillageCode()));
        vo.setRegionPathNames(nonBlankList(entity.getProvinceName(), entity.getCityName(), entity.getDistrictName(), entity.getStreetName(), entity.getVillageName()));
        vo.setDetailAddress(entity.getDetailAddress());
        vo.setFullAddress(entity.getFullAddress());
        vo.setAddressLabel(entity.getAddressLabel());
        vo.setDefaultFlag(Integer.valueOf(1).equals(entity.getDefaultFlag()));
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(StrUtil::trim).filter(StrUtil::isNotBlank).toList();
    }

    private List<String> nonBlankList(String... values) {
        List<String> records = new ArrayList<>();
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                records.add(value);
            }
        }
        return records;
    }

    private String valueAt(List<String> values, int index) {
        return values.size() > index ? values.get(index) : null;
    }

    private String buildFullAddress(List<String> regionNames, String detailAddress) {
        String regionText = String.join("", regionNames.stream()
                .filter(StrUtil::isNotBlank)
                .filter(item -> !"市辖区".equals(item) && !"县".equals(item))
                .toList());
        return StrUtil.trim(regionText + StrUtil.blankToDefault(detailAddress, ""));
    }
}
