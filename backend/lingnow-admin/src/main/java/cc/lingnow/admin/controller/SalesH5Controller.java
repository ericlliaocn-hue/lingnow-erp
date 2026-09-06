package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.shop.ShopAddressSaveBO;
import cc.lingnow.admin.model.bo.shop.ShopOrderSubmitBO;
import cc.lingnow.admin.model.vo.AdminLoginVO;
import cc.lingnow.admin.model.vo.erp.ErpCustomerOrderVO;
import cc.lingnow.admin.model.vo.erp.ErpMasterDataVO;
import cc.lingnow.admin.model.vo.shop.ShopAddressVO;
import cc.lingnow.admin.model.vo.shop.ShopProductVO;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.admin.util.OptionAttributeQuantityUtil;
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
 * 业务员下单 H5。使用 ERP 管理端账号鉴权，地址按业务员会话隔离，订单仍进入现有客户订单链路。
 */
@RestController
@RequestMapping("/sales-h5")
@RequiredArgsConstructor
public class SalesH5Controller {

    private final SysUserService userService;
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

    @GetMapping("/categories")
    public Result<List<ErpMasterDataVO>> categories() {
        currentUser();
        List<ErpProductCategory> categories = categoryService.list(new QueryWrapper<ErpProductCategory>()
                        .eq("status", CommonConstants.STATUS_NORMAL)
                        .orderByAsc("sort_order"));
        Map<Long, ErpProductCategory> categoryById = new HashMap<>();
        categories.forEach(category -> categoryById.put(category.getId(), category));

        Set<Long> visibleCategoryIds = new HashSet<>();
        productService.list(new QueryWrapper<ErpProduct>()
                        .select("category_id")
                        .eq("status", CommonConstants.STATUS_NORMAL)
                        .isNotNull("category_id"))
                .forEach(product -> {
                    Long categoryId = product.getCategoryId();
                    Set<Long> visited = new HashSet<>();
                    while (categoryId != null && categoryId != 0L && visited.add(categoryId)) {
                        visibleCategoryIds.add(categoryId);
                        ErpProductCategory category = categoryById.get(categoryId);
                        if (category == null) break;
                        categoryId = category.getParentId();
                    }
                });

        return Result.success(categories.stream()
                .filter(item -> visibleCategoryIds.contains(item.getId()))
                .map(item -> BeanUtil.copyProperties(item, ErpMasterDataVO.class)).toList());
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

    @GetMapping("/addresses")
    public Result<List<ShopAddressVO>> addresses(@RequestParam(required = false) String keyword) {
        Long salesUserId = currentUserId();
        QueryWrapper<ErpCustomerAddress> wrapper = new QueryWrapper<ErpCustomerAddress>()
                .eq("sales_user_id", salesUserId)
                .orderByDesc("default_flag").orderByDesc("update_time").orderByDesc("create_time");
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(item -> item.like("receiver_name", keyword).or().like("receiver_phone", keyword)
                    .or().like("full_address", keyword).or().like("detail_address", keyword));
        }
        return Result.success(addressService.list(wrapper).stream().map(this::toAddressVO).toList());
    }

    @GetMapping("/addresses/{id}")
    public Result<ShopAddressVO> address(@PathVariable Long id) {
        return Result.success(toAddressVO(requireSalesUserAddress(id)));
    }

    @PostMapping("/addresses")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> createAddress(@Valid @RequestBody ShopAddressSaveBO bo) {
        SysUser user = currentUser();
        ErpCustomer customer = resolveOrCreateCustomer(bo.getReceiverName(), bo.getReceiverPhone(),
                addressText(bo));
        boolean first = addressService.count(new QueryWrapper<ErpCustomerAddress>()
                .eq("sales_user_id", user.getUserId())) == 0;
        boolean makeDefault = first || Boolean.TRUE.equals(bo.getDefaultFlag());
        if (makeDefault) clearSalesUserDefaultAddress(user.getUserId());
        ErpCustomerAddress entity = new ErpCustomerAddress();
        entity.setCustomerId(customer.getId());
        entity.setAccountId(null);
        entity.setSalesUserId(user.getUserId());
        applyAddress(entity, bo);
        entity.setDefaultFlag(makeDefault ? 1 : 0);
        addressService.save(entity);
        return Result.success(toAddressVO(entity));
    }

    @PutMapping("/addresses/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> updateAddress(@PathVariable Long id,
                                               @Valid @RequestBody ShopAddressSaveBO bo) {
        SysUser user = currentUser();
        ErpCustomerAddress entity = requireSalesUserAddress(id);
        ErpCustomer customer = resolveOrCreateCustomer(bo.getReceiverName(), bo.getReceiverPhone(),
                addressText(bo));
        if (Boolean.TRUE.equals(bo.getDefaultFlag())) clearSalesUserDefaultAddress(user.getUserId());
        entity.setCustomerId(customer.getId());
        applyAddress(entity, bo);
        entity.setDefaultFlag(Boolean.TRUE.equals(bo.getDefaultFlag()) ? 1 : 0);
        addressService.updateById(entity);
        ensureSalesUserDefaultAddress(user.getUserId(), id);
        return Result.success(toAddressVO(addressService.getById(id)));
    }

    @PostMapping("/addresses/{id}/default")
    @Transactional(rollbackFor = Exception.class)
    public Result<ShopAddressVO> setDefaultAddress(@PathVariable Long id) {
        SysUser user = currentUser();
        ErpCustomerAddress entity = requireSalesUserAddress(id);
        clearSalesUserDefaultAddress(user.getUserId());
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
        List<ErpCustomerOrderItem> items = buildItems(bo.getItems());
        ReceiverSnapshot receiver = resolveReceiver(bo);
        ErpCustomer customer = resolveOrCreateCustomer(receiver.name(), receiver.phone(), receiver.address());
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
        Map<String, ErpProductAttribute> attributes = activeAttributeMap();
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
            Map<String, BigDecimal> requested = requestedOptionQuantities(source);
            LinkedHashMap<String, BigDecimal> optionQuantities = OptionAttributeQuantityUtil.normalizeRequested(
                    requested, splitIds(source.getOptionAttributeIds()), qty,
                    splitIds(effectiveAttributeIds(product)), splitIds(product.getOptionAttributeIds()), attributes);
            if (requested != null && optionQuantities.size() != requested.size()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "选配项不存在、已停用或不属于当前商品");
            }
            String allowedOptionIds = OptionAttributeQuantityUtil.ids(optionQuantities);
            BigDecimal optionTotal = OptionAttributeQuantityUtil.totalExtraAmount(optionQuantities, attributes);
            BigDecimal amount = basePrice.multiply(qty).add(optionTotal).setScale(4, RoundingMode.HALF_UP);
            BigDecimal price = amount.divide(qty, 4, RoundingMode.HALF_UP);
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
            item.setOptionAttributeText(OptionAttributeQuantityUtil.snapshotText(optionQuantities, attributes));
            item.setOptionAttributeQuantityJson(OptionAttributeQuantityUtil.toJson(optionQuantities));
            item.setUnitId(product.getUnitId());
            item.setQty(qty);
            item.setPrice(price);
            item.setAmount(amount);
            item.setRemark(source.getRemark());
            items.add(item);
        }
        return items;
    }

    private Map<String, ErpProductAttribute> activeAttributeMap() {
        Map<String, ErpProductAttribute> attributes = new LinkedHashMap<>();
        attributeService.list(new QueryWrapper<ErpProductAttribute>().eq("status", CommonConstants.STATUS_NORMAL))
                .forEach(item -> attributes.put(String.valueOf(item.getId()), item));
        return attributes;
    }

    private Map<String, BigDecimal> requestedOptionQuantities(ShopOrderSubmitBO.Item source) {
        if (source.getOptionQuantities() == null) {
            return null;
        }
        Map<String, BigDecimal> requested = new LinkedHashMap<>();
        for (ShopOrderSubmitBO.OptionQuantity option : source.getOptionQuantities()) {
            if (option == null || option.getAttributeId() == null || nvl(option.getQty()).compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "选配项数量必须大于0");
            }
            requested.put(String.valueOf(option.getAttributeId()), option.getQty());
        }
        return requested;
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

    private ReceiverSnapshot resolveReceiver(ShopOrderSubmitBO bo) {
        if (bo.getAddressId() != null) {
            ErpCustomerAddress address = requireSalesUserAddress(bo.getAddressId());
            return new ReceiverSnapshot(address.getReceiverName(), address.getReceiverPhone(),
                    StrUtil.blankToDefault(address.getFullAddress(), address.getDetailAddress()));
        }
        if (StrUtil.isBlank(bo.getReceiverAddress())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写完整收货信息");
        }
        return new ReceiverSnapshot("收货信息", "", StrUtil.trim(bo.getReceiverAddress()));
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

    private ErpProduct requireProduct(Long id) {
        ErpProduct product = productService.getById(id);
        if (product == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品不存在或已停用");
        }
        return product;
    }

    private ErpCustomerAddress requireSalesUserAddress(Long id) {
        ErpCustomerAddress address = addressService.getById(id);
        if (address == null || !Objects.equals(address.getSalesUserId(), currentUserId())) {
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

    private void clearSalesUserDefaultAddress(Long salesUserId) {
        addressService.update(new UpdateWrapper<ErpCustomerAddress>().eq("sales_user_id", salesUserId).set("default_flag", 0));
    }

    private void ensureSalesUserDefaultAddress(Long salesUserId, Long fallbackId) {
        if (addressService.count(new QueryWrapper<ErpCustomerAddress>().eq("sales_user_id", salesUserId).eq("default_flag", 1)) == 0) {
            addressService.update(new UpdateWrapper<ErpCustomerAddress>().eq("id", fallbackId)
                    .eq("sales_user_id", salesUserId).set("default_flag", 1));
        }
    }

    private ErpCustomer resolveOrCreateCustomer(String name, String phone, String address) {
        String normalizedPhone = StrUtil.trim(phone);
        if (StrUtil.isBlank(normalizedPhone)) {
            ErpCustomer guest = customerService.getOne(new QueryWrapper<ErpCustomer>()
                    .eq("code", "SALES_H5_GUEST")
                    .eq("status", CommonConstants.STATUS_NORMAL)
                    .last("limit 1"));
            if (guest != null) {
                return guest;
            }
            ErpCustomer customer = new ErpCustomer();
            customer.setCode("SALES_H5_GUEST");
            customer.setName("销售H5散客");
            customer.setContact("销售H5散客");
            customer.setAddress("收货信息见订单原文");
            customer.setParentId(0L);
            customer.setStatus(CommonConstants.STATUS_NORMAL);
            customer.setSortOrder(0);
            customer.setRemark("销售H5直接填写收货信息");
            customerService.save(customer);
            return customer;
        }
        ErpCustomer existing = customerService.getOne(new QueryWrapper<ErpCustomer>()
                .eq("phone", normalizedPhone)
                .eq("status", CommonConstants.STATUS_NORMAL)
                .orderByDesc("create_time")
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        ErpCustomer customer = new ErpCustomer();
        customer.setCode(nextCustomerCode());
        customer.setName(StrUtil.trim(name));
        customer.setContact(StrUtil.trim(name));
        customer.setPhone(normalizedPhone);
        customer.setAddress(StrUtil.trim(address));
        customer.setParentId(0L);
        customer.setStatus(CommonConstants.STATUS_NORMAL);
        customer.setSortOrder(0);
        customer.setRemark("销售H5自动创建");
        customerService.save(customer);
        return customer;
    }

    private String addressText(ShopAddressSaveBO bo) {
        List<String> names = normalize(bo.getRegionPathNames());
        String regionText = String.join("", names.stream()
                .filter(item -> !"市辖区".equals(item) && !"县".equals(item)).toList());
        return StrUtil.trim(regionText + StrUtil.blankToDefault(bo.getDetailAddress(), ""));
    }

    private String nextCustomerCode() {
        String prefix = "KH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String code = prefix;
        int index = 1;
        while (customerService.count(new QueryWrapper<ErpCustomer>().eq("code", code)) > 0) {
            code = prefix + index++;
        }
        return code;
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
