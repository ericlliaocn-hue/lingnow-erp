package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.shop.ShopAddressSaveBO;
import cc.lingnow.admin.model.bo.shop.ShopOrderSubmitBO;
import cc.lingnow.admin.model.vo.AdminLoginVO;
import cc.lingnow.admin.model.vo.erp.ErpCustomerOrderVO;
import cc.lingnow.admin.model.vo.erp.ErpMasterDataVO;
import cc.lingnow.admin.model.vo.shop.ShopAddressVO;
import cc.lingnow.admin.model.vo.shop.ShopProductVO;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.model.ErpAddressParseBO;
import cc.lingnow.biz.erp.model.ErpAddressParseVO;
import cc.lingnow.biz.erp.model.ErpAddressRegionVO;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.biz.file.service.SysFileService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.constant.CommonConstants;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 业务员代客下单 H5。使用 ERP 管理端账号鉴权，订单仍进入现有客户订单链路。
 */
@RestController
@RequestMapping("/sales-h5")
@RequiredArgsConstructor
public class SalesH5Controller {

    private final SysUserService userService;
    private final ErpDataAuthService dataAuthService;
    private final ErpCustomerService customerService;
    private final ErpCustomerAddressService addressService;
    private final ErpProductService productService;
    private final ErpProductCategoryService categoryService;
    private final ErpProductAttributeService attributeService;
    private final ErpUnitService unitService;
    private final ErpCustomerOrderService orderService;
    private final ErpCustomerOrderItemService orderItemService;
    private final ErpAddressRegionService addressRegionService;
    private final ErpAddressParseService addressParseService;
    private final SysFileService fileService;

    @GetMapping("/me")
    public Result<AdminLoginVO> me() {
        SysUser user = currentUser();
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(StpAdminUtil.getTokenValue());
        vo.setUsername(user.getUsername());
        vo.setNickname(StrUtil.blankToDefault(user.getNickname(), user.getUsername()));
        vo.setPermissions(StpAdminUtil.stpLogic.getPermissionList());
        return Result.success(vo);
    }

    @GetMapping("/customers")
    public Result<List<Map<String, Object>>> customers(@RequestParam(required = false) String keyword) {
        Long userId = currentUserId();
        QueryWrapper<ErpCustomer> wrapper = new QueryWrapper<ErpCustomer>()
                .eq("status", CommonConstants.STATUS_NORMAL)
                .and(StrUtil.isNotBlank(keyword), item -> item.like("name", keyword)
                        .or().like("contact", keyword)
                        .or().like("phone", keyword))
                .orderByAsc("sort_order")
                .orderByDesc("create_time");
        List<Long> authorizedIds = dataAuthService.authorizedIds(userId, "CUSTOMER");
        if (!authorizedIds.isEmpty()) {
            wrapper.in("id", authorizedIds);
        }
        return Result.success(customerService.list(wrapper).stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("code", item.getCode());
            row.put("name", item.getName());
            row.put("contact", item.getContact());
            row.put("phone", item.getPhone());
            row.put("address", item.getAddress());
            return row;
        }).toList());
    }

    @GetMapping("/categories")
    public Result<List<ErpMasterDataVO>> categories() {
        currentUser();
        return Result.success(categoryService.list(new QueryWrapper<ErpProductCategory>()
                        .eq("status", CommonConstants.STATUS_NORMAL)
                        .orderByAsc("sort_order"))
                .stream().map(item -> BeanUtil.copyProperties(item, ErpMasterDataVO.class)).toList());
    }

    @GetMapping("/products")
    public Result<PageResult<ShopProductVO>> products(@RequestParam(defaultValue = "1") Long current,
                                                      @RequestParam(defaultValue = "20") Long size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long categoryId) {
        currentUser();
        QueryWrapper<ErpProduct> wrapper = new QueryWrapper<ErpProduct>()
                .eq("status", CommonConstants.STATUS_NORMAL)
                .in(categoryId != null, "category_id", categoryIds(categoryId));
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(item -> item.like("name", keyword).or().like("code", keyword)
                    .or().like("barcode", keyword).or().like("spec", keyword)
                    .or().like("attribute_text", keyword));
        }
        wrapper.orderByAsc("sort_order").orderByDesc("create_time");
        Page<ErpProduct> page = productService.page(new Page<>(current, size), wrapper);
        List<ShopProductVO> records = page.getRecords().stream().map(this::toProductVO).toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @GetMapping("/products/{id}")
    public Result<ShopProductVO> product(@PathVariable Long id) {
        currentUser();
        return Result.success(toProductVO(requireProduct(id)));
    }

    @GetMapping("/attributes")
    public Result<List<ErpMasterDataVO>> attributes() {
        currentUser();
        return Result.success(attributeService.list(new QueryWrapper<ErpProductAttribute>()
                        .eq("status", CommonConstants.STATUS_NORMAL).orderByAsc("sort_order"))
                .stream().map(item -> BeanUtil.copyProperties(item, ErpMasterDataVO.class)).toList());
    }

    @GetMapping("/customers/{customerId}/addresses")
    public Result<List<ShopAddressVO>> addresses(@PathVariable Long customerId,
                                                 @RequestParam(required = false) String keyword) {
        requireCustomer(customerId);
        QueryWrapper<ErpCustomerAddress> wrapper = new QueryWrapper<ErpCustomerAddress>()
                .eq("customer_id", customerId)
                .orderByDesc("default_flag").orderByDesc("update_time").orderByDesc("create_time");
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(item -> item.like("receiver_name", keyword).or().like("receiver_phone", keyword)
                    .or().like("full_address", keyword).or().like("detail_address", keyword));
        }
        return Result.success(addressService.list(wrapper).stream().map(this::toAddressVO).toList());
    }

    @GetMapping("/customers/{customerId}/addresses/{id}")
    public Result<ShopAddressVO> address(@PathVariable Long customerId, @PathVariable Long id) {
        requireCustomer(customerId);
        return Result.success(toAddressVO(requireCustomerAddress(customerId, id)));
    }

    @PostMapping("/customers/{customerId}/addresses")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> createAddress(@PathVariable Long customerId,
                                               @Valid @RequestBody ShopAddressSaveBO bo) {
        requireCustomer(customerId);
        boolean first = addressService.count(new QueryWrapper<ErpCustomerAddress>().eq("customer_id", customerId)) == 0;
        boolean makeDefault = first || Boolean.TRUE.equals(bo.getDefaultFlag());
        if (makeDefault) clearCustomerDefaultAddress(customerId);
        ErpCustomerAddress entity = new ErpCustomerAddress();
        entity.setCustomerId(customerId);
        entity.setAccountId(null);
        applyAddress(entity, bo);
        entity.setDefaultFlag(makeDefault ? 1 : 0);
        addressService.save(entity);
        return Result.success(toAddressVO(entity));
    }

    @PutMapping("/customers/{customerId}/addresses/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> updateAddress(@PathVariable Long customerId, @PathVariable Long id,
                                               @Valid @RequestBody ShopAddressSaveBO bo) {
        requireCustomer(customerId);
        ErpCustomerAddress entity = requireCustomerAddress(customerId, id);
        if (Boolean.TRUE.equals(bo.getDefaultFlag())) clearCustomerDefaultAddress(customerId);
        applyAddress(entity, bo);
        entity.setDefaultFlag(Boolean.TRUE.equals(bo.getDefaultFlag()) ? 1 : 0);
        addressService.updateById(entity);
        ensureCustomerDefaultAddress(customerId, id);
        return Result.success(toAddressVO(addressService.getById(id)));
    }

    @PostMapping("/customers/{customerId}/addresses/{id}/default")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> setDefaultAddress(@PathVariable Long customerId, @PathVariable Long id) {
        requireCustomer(customerId);
        ErpCustomerAddress entity = requireCustomerAddress(customerId, id);
        clearCustomerDefaultAddress(customerId);
        entity.setDefaultFlag(1);
        addressService.updateById(entity);
        return Result.success(toAddressVO(entity));
    }

    @GetMapping("/address/regions")
    public Result<List<ErpAddressRegionVO>> addressRegions(@RequestParam(required = false) String parentCode) {
        currentUser();
        return Result.success(addressRegionService.listChildren(parentCode));
    }

    @GetMapping("/address/search")
    public Result<List<ErpAddressRegionVO>> searchAddressRegions(@RequestParam String keyword,
                                                                 @RequestParam(defaultValue = "20") Integer limit) {
        currentUser();
        return Result.success(addressRegionService.search(keyword, limit == null ? 20 : limit));
    }

    @PostMapping("/address/parse")
    public Result<ErpAddressParseVO> parseAddress(@Valid @RequestBody ErpAddressParseBO bo) {
        currentUser();
        return Result.success(addressParseService.parse(bo.getRawText()));
    }

    @PostMapping("/file/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        currentUser();
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_ERROR, "请选择图片文件");
        }
        if (file.getSize() > 20L * 1024L * 1024L) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "图片不能超过20MB");
        }
        return Result.success(fileService.upload(file));
    }

    @PostMapping("/orders")
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> submitOrder(@Valid @RequestBody ShopOrderSubmitBO bo) {
        SysUser user = currentUser();
        ErpCustomer customer = requireCustomer(bo.getCustomerId());
        List<ErpCustomerOrderItem> items = buildItems(bo.getItems());
        ReceiverSnapshot receiver = resolveReceiver(bo, customer.getId());
        BigDecimal totalQty = items.stream().map(item -> nvl(item.getQty())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = items.stream().map(item -> nvl(item.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

        ErpCustomerOrder order = new ErpCustomerOrder();
        order.setOrderNo(nextOrderNo());
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setAccountId(null);
        order.setAccountName(displayName(user));
        order.setEmployeeId(user.getUserId());
        order.setEmployeeName(displayName(user));
        order.setSource("SALES_H5");
        order.setStatus("PENDING");
        order.setOrderTime(LocalDateTime.now());
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setReceiverName(receiver.name());
        order.setReceiverPhone(receiver.phone());
        order.setReceiverAddress(receiver.address());
        order.setRemark(bo.getRemark());
        orderService.save(order);
        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemService.saveBatch(items);
        return Result.success(order.getId());
    }

    @GetMapping("/orders")
    public Result<PageResult<ErpCustomerOrderVO>> orders(@RequestParam(defaultValue = "1") Long current,
                                                        @RequestParam(defaultValue = "10") Long size) {
        Long employeeId = currentUserId();
        Page<ErpCustomerOrder> page = orderService.page(new Page<>(current, size),
                new QueryWrapper<ErpCustomerOrder>().eq("employee_id", employeeId).orderByDesc("create_time"));
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(item -> toOrderVO(item, false)).toList()));
    }

    @GetMapping("/orders/{id}")
    public Result<ErpCustomerOrderVO> order(@PathVariable Long id) {
        Long employeeId = currentUserId();
        ErpCustomerOrder order = orderService.getById(id);
        if (order == null || !Objects.equals(order.getEmployeeId(), employeeId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return Result.success(toOrderVO(order, true));
    }

    private List<ErpCustomerOrderItem> buildItems(List<ShopOrderSubmitBO.Item> sourceItems) {
        List<ErpCustomerOrderItem> items = new ArrayList<>();
        for (ShopOrderSubmitBO.Item source : sourceItems) {
            ErpProduct product = requireProduct(source.getProductId());
            BigDecimal qty = nvl(source.getQty());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "数量必须大于0");
            }
            BigDecimal basePrice = nvl(product.getSalePrice());
            if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        product.getName() + "销售价未维护，请先在商品管理中补全");
            }
            String allowedOptionIds = allowedOptionIds(product, source.getOptionAttributeIds());
            BigDecimal price = basePrice.add(attributeExtraAmount(allowedOptionIds));
            ErpCustomerOrderItem item = new ErpCustomerOrderItem();
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setProductImageUrl(product.getImageUrl());
            item.setLogoImageUrl(source.getLogoImageUrl());
            item.setSpec(product.getSpec());
            item.setAttributeText(product.getAttributeText());
            applyProductCategory(item, product);
            item.setOptionAttributeIds(allowedOptionIds);
            item.setOptionAttributeText(optionText(allowedOptionIds));
            item.setUnitId(product.getUnitId());
            item.setQty(qty);
            item.setPrice(price);
            item.setAmount(qty.multiply(price).setScale(4, RoundingMode.HALF_UP));
            item.setRemark(source.getRemark());
            items.add(item);
        }
        return items;
    }

    private String allowedOptionIds(ErpProduct product, String optionIds) {
        Set<String> allowedGroups = splitIds(effectiveAttributeIds(product));
        Map<String, ErpProductAttribute> attributes = new HashMap<>();
        attributeService.list(new QueryWrapper<ErpProductAttribute>().eq("status", CommonConstants.STATUS_NORMAL))
                .forEach(item -> attributes.put(String.valueOf(item.getId()), item));
        Map<String, String> selectedByGroup = new LinkedHashMap<>();
        for (String id : splitIds(optionIds)) {
            ErpProductAttribute option = attributes.get(id);
            if (option != null && allowedGroups.contains(String.valueOf(option.getParentId()))) {
                selectedByGroup.put(String.valueOf(option.getParentId()), id);
            }
        }
        return String.join(",", selectedByGroup.values());
    }

    private BigDecimal attributeExtraAmount(String optionIds) {
        return splitIds(optionIds).stream().map(this::parseLong).filter(Objects::nonNull)
                .map(attributeService::getById).filter(Objects::nonNull)
                .map(item -> nvl(item.getExtraAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String optionText(String optionIds) {
        Map<String, ErpProductAttribute> byId = new HashMap<>();
        attributeService.list().forEach(item -> byId.put(String.valueOf(item.getId()), item));
        return splitIds(optionIds).stream().map(byId::get).filter(Objects::nonNull).map(option -> {
            ErpProductAttribute group = byId.get(String.valueOf(option.getParentId()));
            return (group == null ? "商品属性" : group.getName()) + ": " + option.getName();
        }).reduce((left, right) -> left + " / " + right).orElse("");
    }

    private ReceiverSnapshot resolveReceiver(ShopOrderSubmitBO bo, Long customerId) {
        if (bo.getAddressId() != null) {
            ErpCustomerAddress address = requireCustomerAddress(customerId, bo.getAddressId());
            return new ReceiverSnapshot(address.getReceiverName(), address.getReceiverPhone(),
                    StrUtil.blankToDefault(address.getFullAddress(), address.getDetailAddress()));
        }
        if (StrUtil.isBlank(bo.getReceiverName()) || StrUtil.isBlank(bo.getReceiverPhone())
                || StrUtil.isBlank(bo.getReceiverAddress())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收货信息不能为空");
        }
        return new ReceiverSnapshot(bo.getReceiverName(), bo.getReceiverPhone(), bo.getReceiverAddress());
    }

    private ShopProductVO toProductVO(ErpProduct product) {
        ShopProductVO vo = BeanUtil.copyProperties(product, ShopProductVO.class);
        ErpUnit unit = product.getUnitId() == null ? null : unitService.getById(product.getUnitId());
        ErpProductCategory category = product.getCategoryId() == null ? null : categoryService.getById(product.getCategoryId());
        vo.setUnitName(unit == null ? null : unit.getName());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setAttributeIds(effectiveAttributeIds(product));
        return vo;
    }

    private String effectiveAttributeIds(ErpProduct product) {
        if (StrUtil.isNotBlank(product.getAttributeIds())) return product.getAttributeIds();
        Long categoryId = product.getCategoryId();
        Set<Long> visited = new HashSet<>();
        while (categoryId != null && categoryId != 0L && visited.add(categoryId)) {
            ErpProductCategory category = categoryService.getById(categoryId);
            if (category == null) break;
            if (StrUtil.isNotBlank(category.getAttributeIds())) return category.getAttributeIds();
            categoryId = category.getParentId();
        }
        return "";
    }

    private Set<Long> categoryIds(Long rootId) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(rootId);
        List<ErpProductCategory> all = categoryService.list(new QueryWrapper<ErpProductCategory>()
                .eq("status", CommonConstants.STATUS_NORMAL));
        boolean changed;
        do {
            changed = false;
            for (ErpProductCategory item : all) {
                if (ids.contains(item.getParentId()) && ids.add(item.getId())) changed = true;
            }
        } while (changed);
        return ids;
    }

    private void applyProductCategory(ErpCustomerOrderItem item, ErpProduct product) {
        if (product.getCategoryId() == null) return;
        List<ErpProductCategory> path = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Long categoryId = product.getCategoryId();
        while (categoryId != null && categoryId != 0L && visited.add(categoryId)) {
            ErpProductCategory category = categoryService.getById(categoryId);
            if (category == null) break;
            path.add(category);
            categoryId = category.getParentId();
        }
        if (path.isEmpty()) return;
        Collections.reverse(path);
        ErpProductCategory level1 = path.get(0);
        item.setCategoryLevel1Id(level1.getId());
        item.setCategoryLevel1Name(level1.getName());
        if (path.size() > 1) {
            ErpProductCategory level2 = path.get(1);
            item.setCategoryLevel2Id(level2.getId());
            item.setCategoryLevel2Name(level2.getName());
        }
    }

    private ErpCustomerOrderVO toOrderVO(ErpCustomerOrder order, boolean withItems) {
        ErpCustomerOrderVO vo = BeanUtil.copyProperties(order, ErpCustomerOrderVO.class);
        if (withItems) {
            vo.setItems(orderItemService.list(new QueryWrapper<ErpCustomerOrderItem>().eq("order_id", order.getId()))
                    .stream().map(item -> BeanUtil.copyProperties(item, ErpCustomerOrderVO.Item.class)).toList());
        }
        return vo;
    }

    private ErpCustomer requireCustomer(Long id) {
        currentUser();
        if (id == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择客户");
        ErpCustomer customer = customerService.getById(id);
        if (customer == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(customer.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "客户不存在或已停用");
        }
        List<Long> authorizedIds = dataAuthService.authorizedIds(currentUserId(), "CUSTOMER");
        if (!authorizedIds.isEmpty() && !authorizedIds.contains(id)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权为该客户下单");
        }
        return customer;
    }

    private ErpProduct requireProduct(Long id) {
        ErpProduct product = productService.getById(id);
        if (product == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品不存在或已停用");
        }
        return product;
    }

    private ErpCustomerAddress requireCustomerAddress(Long customerId, Long id) {
        ErpCustomerAddress address = addressService.getById(id);
        if (address == null || !Objects.equals(address.getCustomerId(), customerId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "收货地址不存在");
        }
        return address;
    }

    private SysUser currentUser() {
        StpAdminUtil.checkLogin();
        SysUser user = userService.getById(currentUserId());
        if (user == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return user;
    }

    private Long currentUserId() {
        Object loginId = StpAdminUtil.getLoginIdDefaultNull();
        if (loginId == null) throw new BusinessException(ErrorCode.NOT_LOGIN);
        return Long.valueOf(String.valueOf(loginId));
    }

    private String displayName(SysUser user) {
        return StrUtil.blankToDefault(user.getNickname(), user.getUsername());
    }

    private void applyAddress(ErpCustomerAddress entity, ShopAddressSaveBO bo) {
        List<String> path = normalize(bo.getRegionPath());
        List<String> names = normalize(bo.getRegionPathNames());
        entity.setReceiverName(StrUtil.trim(bo.getReceiverName()));
        entity.setReceiverPhone(StrUtil.trim(bo.getReceiverPhone()));
        entity.setProvinceCode(valueAt(path, 0)); entity.setProvinceName(valueAt(names, 0));
        entity.setCityCode(valueAt(path, 1)); entity.setCityName(valueAt(names, 1));
        entity.setDistrictCode(valueAt(path, 2)); entity.setDistrictName(valueAt(names, 2));
        entity.setStreetCode(valueAt(path, 3)); entity.setStreetName(valueAt(names, 3));
        entity.setVillageCode(valueAt(path, 4)); entity.setVillageName(valueAt(names, 4));
        entity.setDetailAddress(StrUtil.trim(bo.getDetailAddress()));
        entity.setAddressLabel(StrUtil.trim(bo.getAddressLabel()));
        String regionText = String.join("", names.stream().filter(item -> !"市辖区".equals(item) && !"县".equals(item)).toList());
        entity.setFullAddress(StrUtil.trim(regionText + StrUtil.blankToDefault(bo.getDetailAddress(), "")));
    }

    private ShopAddressVO toAddressVO(ErpCustomerAddress entity) {
        ShopAddressVO vo = new ShopAddressVO();
        BeanUtil.copyProperties(entity, vo);
        vo.setRegionPath(nonBlank(entity.getProvinceCode(), entity.getCityCode(), entity.getDistrictCode(), entity.getStreetCode(), entity.getVillageCode()));
        vo.setRegionPathNames(nonBlank(entity.getProvinceName(), entity.getCityName(), entity.getDistrictName(), entity.getStreetName(), entity.getVillageName()));
        vo.setDefaultFlag(Integer.valueOf(1).equals(entity.getDefaultFlag()));
        return vo;
    }

    private void clearCustomerDefaultAddress(Long customerId) {
        addressService.update(new UpdateWrapper<ErpCustomerAddress>().eq("customer_id", customerId).set("default_flag", 0));
    }

    private void ensureCustomerDefaultAddress(Long customerId, Long fallbackId) {
        if (addressService.count(new QueryWrapper<ErpCustomerAddress>().eq("customer_id", customerId).eq("default_flag", 1)) == 0) {
            addressService.update(new UpdateWrapper<ErpCustomerAddress>().eq("id", fallbackId)
                    .eq("customer_id", customerId).set("default_flag", 1));
        }
    }

    private List<String> normalize(List<String> values) {
        return values == null ? List.of() : values.stream().map(StrUtil::trim).filter(StrUtil::isNotBlank).toList();
    }

    private List<String> nonBlank(String... values) {
        return Arrays.stream(values).filter(StrUtil::isNotBlank).toList();
    }

    private String valueAt(List<String> values, int index) { return values.size() > index ? values.get(index) : null; }

    private Set<String> splitIds(String value) {
        if (StrUtil.isBlank(value)) return Set.of();
        Set<String> ids = new LinkedHashSet<>();
        for (String item : value.split(",")) if (StrUtil.isNotBlank(item)) ids.add(item.trim());
        return ids;
    }

    private Long parseLong(String value) {
        try { return Long.valueOf(value); } catch (NumberFormatException e) { return null; }
    }

    private String nextOrderNo() {
        String prefix = "YW-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long serial = orderService.count(new QueryWrapper<ErpCustomerOrder>().likeRight("order_no", prefix)) + 1;
        String orderNo;
        do { orderNo = prefix + String.format("%04d", serial++); }
        while (orderService.count(new QueryWrapper<ErpCustomerOrder>().eq("order_no", orderNo)) > 0);
        return orderNo;
    }

    private BigDecimal nvl(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private record ReceiverSnapshot(String name, String phone, String address) {}
}
