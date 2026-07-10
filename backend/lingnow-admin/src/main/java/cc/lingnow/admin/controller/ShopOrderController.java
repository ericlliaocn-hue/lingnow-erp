package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.shop.ShopOrderSubmitBO;
import cc.lingnow.admin.model.vo.erp.ErpCustomerOrderVO;
import cc.lingnow.admin.model.vo.erp.ErpMasterDataVO;
import cc.lingnow.admin.model.vo.shop.ShopProductVO;
import cc.lingnow.admin.util.StpShopUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.model.ErpAddressRegionVO;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.biz.file.service.SysFileService;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/shop-api")
@RequiredArgsConstructor
public class ShopOrderController {

    private final ErpCustomerAccountService accountService;
    private final ErpCustomerService customerService;
    private final ErpProductService productService;
    private final ErpProductAttributeService attributeService;
    private final ErpUnitService unitService;
    private final ErpProductCategoryService categoryService;
    private final ErpCustomerOrderService orderService;
    private final ErpCustomerOrderItemService orderItemService;
    private final ErpAddressRegionService addressRegionService;
    private final SysFileService fileService;

    @GetMapping("/products")
    public Result<PageResult<ShopProductVO>> products(@RequestParam(defaultValue = "1") Long current,
                                                      @RequestParam(defaultValue = "20") Long size,
                                                      @RequestParam(required = false) String keyword) {
        QueryWrapper<ErpProduct> wrapper = new QueryWrapper<ErpProduct>()
                .eq("status", CommonConstants.STATUS_NORMAL)
                .orderByAsc("sort_order")
                .orderByDesc("create_time");
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(item -> item.like("code", keyword).or().like("name", keyword).or().like("barcode", keyword));
        }
        Page<ErpProduct> page = productService.page(new Page<>(current, size), wrapper);
        List<ShopProductVO> records = page.getRecords().stream().map(this::toShopProductVO).toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @GetMapping("/products/{id}")
    public Result<ShopProductVO> product(@PathVariable Long id) {
        ErpProduct product = requireEnabledProduct(id);
        return Result.success(toShopProductVO(product));
    }

    @GetMapping("/attributes")
    public Result<List<ErpMasterDataVO>> attributes() {
        currentAccount();
        List<ErpMasterDataVO> records = attributeService.list(new QueryWrapper<ErpProductAttribute>()
                        .eq("status", CommonConstants.STATUS_NORMAL)
                        .orderByAsc("sort_order"))
                .stream()
                .map(item -> BeanUtil.copyProperties(item, ErpMasterDataVO.class))
                .toList();
        return Result.success(records);
    }

    @GetMapping("/address/regions")
    public Result<List<ErpAddressRegionVO>> addressRegions(@RequestParam(required = false) String parentCode) {
        currentAccount();
        return Result.success(addressRegionService.listChildren(parentCode));
    }

    @GetMapping("/address/search")
    public Result<List<ErpAddressRegionVO>> searchAddressRegions(@RequestParam String keyword,
                                                                 @RequestParam(defaultValue = "20") Integer limit) {
        currentAccount();
        return Result.success(addressRegionService.search(keyword, limit == null ? 20 : limit));
    }

    @PostMapping("/file/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        currentAccount();
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_ERROR, "请选择图片文件");
        }
        if (file.getSize() > 20L * 1024L * 1024L) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "图片不能超过20MB");
        }
        return Result.success(fileService.upload(file));
    }

    @GetMapping("/orders")
    public Result<PageResult<ErpCustomerOrderVO>> orders(@RequestParam(defaultValue = "1") Long current,
                                                        @RequestParam(defaultValue = "10") Long size) {
        ErpCustomerAccount account = currentAccount();
        Page<ErpCustomerOrder> page = orderService.page(new Page<>(current, size), new QueryWrapper<ErpCustomerOrder>()
                .eq("account_id", account.getId())
                .orderByDesc("create_time"));
        List<ErpCustomerOrderVO> records = page.getRecords().stream().map(item -> toOrderVO(item, false)).toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @GetMapping("/orders/{id}")
    public Result<ErpCustomerOrderVO> order(@PathVariable Long id) {
        ErpCustomerAccount account = currentAccount();
        ErpCustomerOrder order = orderService.getById(id);
        if (order == null || !Objects.equals(order.getAccountId(), account.getId())) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return Result.success(toOrderVO(order, true));
    }

    @PostMapping("/orders")
    public Result<Long> submitOrder(@Valid @RequestBody ShopOrderSubmitBO bo) {
        ErpCustomerAccount account = currentAccount();
        ErpCustomer customer = customerService.getById(account.getCustomerId());
        if (customer == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(customer.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "客户不存在或已停用");
        }
        List<ErpCustomerOrderItem> items = buildItems(bo.getItems());
        BigDecimal totalQty = items.stream().map(item -> nvl(item.getQty())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = items.stream().map(item -> nvl(item.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

        ErpCustomerOrder order = new ErpCustomerOrder();
        order.setOrderNo(nextOrderNo());
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setAccountId(account.getId());
        order.setAccountName(StrUtil.blankToDefault(account.getNickname(), account.getUsername()));
        order.setStatus("PENDING");
        order.setOrderTime(LocalDateTime.now());
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setReceiverName(bo.getReceiverName());
        order.setReceiverPhone(bo.getReceiverPhone());
        order.setReceiverAddress(bo.getReceiverAddress());
        order.setRemark(bo.getRemark());
        orderService.save(order);
        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemService.saveBatch(items);
        return Result.success(order.getId());
    }

    private ErpCustomerAccount currentAccount() {
        StpShopUtil.checkLogin();
        ErpCustomerAccount account = accountService.getById(StpShopUtil.getLoginIdAsLong());
        if (account == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return account;
    }

    private List<ErpCustomerOrderItem> buildItems(List<ShopOrderSubmitBO.Item> sourceItems) {
        List<ErpCustomerOrderItem> items = new ArrayList<>();
        for (ShopOrderSubmitBO.Item source : sourceItems) {
            ErpProduct product = requireEnabledProduct(source.getProductId());
            BigDecimal qty = nvl(source.getQty());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "数量必须大于0");
            }
            String allowedOptionIds = allowedOptionIds(product, source.getOptionAttributeIds());
            BigDecimal price = nvl(product.getSalePrice()).add(attributeExtraAmount(allowedOptionIds));
            BigDecimal amount = qty.multiply(price).setScale(4, RoundingMode.HALF_UP);
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
            item.setOptionAttributeText(optionText(item.getOptionAttributeIds()));
            item.setUnitId(product.getUnitId());
            item.setQty(qty);
            item.setPrice(price);
            item.setAmount(amount);
            item.setRemark(source.getRemark());
            items.add(item);
        }
        return items;
    }

    private String allowedOptionIds(ErpProduct product, String optionIds) {
        Set<String> allowedGroups = splitIds(product.getAttributeIds());
        if (allowedGroups.isEmpty()) {
            return "";
        }
        Map<String, ErpProductAttribute> optionById = new HashMap<>();
        attributeService.list(new QueryWrapper<ErpProductAttribute>().eq("status", CommonConstants.STATUS_NORMAL))
                .forEach(item -> optionById.put(String.valueOf(item.getId()), item));
        List<String> selected = splitIds(optionIds).stream()
                .filter(id -> {
                    ErpProductAttribute option = optionById.get(id);
                    return option != null && allowedGroups.contains(String.valueOf(option.getParentId()));
                })
                .toList();
        return String.join(",", selected);
    }

    private BigDecimal attributeExtraAmount(String optionIds) {
        BigDecimal amount = BigDecimal.ZERO;
        for (String id : splitIds(optionIds)) {
            Long attributeId = parseId(id);
            if (attributeId == null) {
                continue;
            }
            ErpProductAttribute attribute = attributeService.getById(attributeId);
            if (attribute != null && attribute.getExtraAmount() != null) {
                amount = amount.add(attribute.getExtraAmount());
            }
        }
        return amount;
    }

    private String optionText(String optionIds) {
        Map<String, ErpProductAttribute> byId = new HashMap<>();
        attributeService.list().forEach(item -> byId.put(String.valueOf(item.getId()), item));
        List<String> values = new ArrayList<>();
        for (String id : splitIds(optionIds)) {
            ErpProductAttribute option = byId.get(id);
            if (option == null) {
                continue;
            }
            ErpProductAttribute group = byId.get(String.valueOf(option.getParentId()));
            values.add((group == null ? "商品属性" : group.getName()) + ": " + option.getName());
        }
        return String.join(" / ", values);
    }

    private Set<String> splitIds(String value) {
        if (StrUtil.isBlank(value)) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            if (StrUtil.isNotBlank(item)) {
                ids.add(item.trim());
            }
        }
        return ids;
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void applyProductCategory(ErpCustomerOrderItem item, ErpProduct product) {
        if (product.getCategoryId() == null) {
            return;
        }
        ErpProductCategory leaf = categoryService.getById(product.getCategoryId());
        if (leaf == null) {
            return;
        }
        ErpProductCategory parent = leaf.getParentId() == null || Long.valueOf(0L).equals(leaf.getParentId()) ? null : categoryService.getById(leaf.getParentId());
        if (parent == null) {
            item.setCategoryLevel1Id(leaf.getId());
            item.setCategoryLevel1Name(leaf.getName());
        } else {
            item.setCategoryLevel1Id(parent.getId());
            item.setCategoryLevel1Name(parent.getName());
            item.setCategoryLevel2Id(leaf.getId());
            item.setCategoryLevel2Name(leaf.getName());
        }
    }

    private ErpCustomerOrderVO toOrderVO(ErpCustomerOrder order, boolean withItems) {
        ErpCustomerOrderVO vo = BeanUtil.copyProperties(order, ErpCustomerOrderVO.class);
        if (withItems) {
            vo.setItems(orderItemService.list(new QueryWrapper<ErpCustomerOrderItem>().eq("order_id", order.getId()))
                    .stream()
                    .map(item -> BeanUtil.copyProperties(item, ErpCustomerOrderVO.Item.class))
                    .toList());
        }
        return vo;
    }

    private ShopProductVO toShopProductVO(ErpProduct product) {
        ShopProductVO vo = BeanUtil.copyProperties(product, ShopProductVO.class);
        ErpUnit unit = product.getUnitId() == null ? null : unitService.getById(product.getUnitId());
        vo.setUnitName(unit == null ? null : unit.getName());
        return vo;
    }

    private ErpProduct requireEnabledProduct(Long id) {
        ErpProduct product = productService.getById(id);
        if (product == null || !Integer.valueOf(CommonConstants.STATUS_NORMAL).equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品不存在或已停用");
        }
        return product;
    }

    private String nextOrderNo() {
        String prefix = "KH-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long serial = orderService.count(new QueryWrapper<ErpCustomerOrder>().likeRight("order_no", prefix)) + 1;
        String orderNo;
        do {
            orderNo = prefix + String.format("%04d", serial++);
        } while (orderService.count(new QueryWrapper<ErpCustomerOrder>().eq("order_no", orderNo)) > 0);
        return orderNo;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
