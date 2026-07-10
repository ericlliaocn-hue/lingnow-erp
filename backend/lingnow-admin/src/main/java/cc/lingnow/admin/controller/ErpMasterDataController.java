package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpMasterDataQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpMasterDataSaveBO;
import cc.lingnow.admin.model.vo.erp.ErpMasterDataVO;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cc.lingnow.admin.util.StpAdminUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "ERP基础资料")
@RestController
@RequestMapping("/erp/master")
@RequiredArgsConstructor
public class ErpMasterDataController {

    private final ErpProductCategoryService productCategoryService;
    private final ErpUnitService unitService;
    private final ErpProductBrandService productBrandService;
    private final ErpProductAttributeService productAttributeService;
    private final ErpCustomerService customerService;
    private final ErpSupplierService supplierService;
    private final ErpWarehouseService warehouseService;
    private final ErpAccountService accountService;
    private final ErpAgentLevelService agentLevelService;
    private final ErpProductService productService;
    private final ErpBillService billService;
    private final ErpBillItemService billItemService;
    private final ErpStockBalanceService stockBalanceService;
    private final ErpStockFlowService stockFlowService;
    private final ErpStockCheckService stockCheckService;
    private final ErpStockCheckItemService stockCheckItemService;
    private final ErpFinanceBillService financeBillService;
    private final ErpFundFlowService fundFlowService;
    private final ErpPartnerFlowService partnerFlowService;

    @Operation(summary = "ERP基础资料列表")
    @GetMapping("/{type}/list")
    public Result<PageResult<ErpMasterDataVO>> list(@PathVariable String type, ErpMasterDataQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission(permission(type, "list"));
        IService service = service(type);
        IPage<ErpMasterData> page = service.page(new Page<>(query.getCurrent(), query.getSize()), wrapper(query));
        List<ErpMasterDataVO> records = page.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, ErpMasterDataVO.class))
                .toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @Operation(summary = "ERP基础资料详情")
    @GetMapping("/{type}/{id}")
    public Result<ErpMasterDataVO> getInfo(@PathVariable String type, @PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission(permission(type, "list"));
        ErpMasterData entity = (ErpMasterData) service(type).getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return Result.success(BeanUtil.copyProperties(entity, ErpMasterDataVO.class));
    }

    @Operation(summary = "ERP基础资料新增")
    @PostMapping("/{type}")
    @Log(title = "ERP基础资料", businessType = BusinessType.INSERT)
    public Result<Void> add(@PathVariable String type, @Valid @RequestBody ErpMasterDataSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission(permission(type, "add"));
        IService service = service(type);
        ensureCodeUnique(service, bo.getCode(), null);
        ensureValidParent(type, bo.getParentId(), null);
        service.save(toEntity(type, bo));
        return Result.success();
    }

    @Operation(summary = "ERP基础资料修改")
    @PutMapping("/{type}")
    @Log(title = "ERP基础资料", businessType = BusinessType.UPDATE)
    public Result<Void> edit(@PathVariable String type, @Valid @RequestBody ErpMasterDataSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        StpAdminUtil.stpLogic.checkPermission(permission(type, "edit"));
        IService service = service(type);
        ensureCodeUnique(service, bo.getCode(), bo.getId());
        ensureValidParent(type, bo.getParentId(), bo.getId());
        service.updateById(toEntity(type, bo));
        return Result.success();
    }

    @Operation(summary = "ERP基础资料删除")
    @DeleteMapping("/{type}/{ids}")
    @Log(title = "ERP基础资料", businessType = BusinessType.DELETE)
    public Result<Void> remove(@PathVariable String type, @PathVariable List<Long> ids) {
        StpAdminUtil.stpLogic.checkPermission(permission(type, "remove"));
        ensureNotReferenced(type, ids);
        service(type).removeByIds(ids);
        return Result.success();
    }

    private QueryWrapper<ErpMasterData> wrapper(ErpMasterDataQueryBO query) {
        QueryWrapper<ErpMasterData> wrapper = new QueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getCode()), "code", query.getCode())
                .like(StrUtil.isNotBlank(query.getName()), "name", query.getName())
                .eq(query.getStatus() != null, "status", query.getStatus())
                .like(StrUtil.isNotBlank(query.getContact()), "contact", query.getContact())
                .like(StrUtil.isNotBlank(query.getPhone()), "phone", query.getPhone())
                .orderByAsc("sort_order")
                .orderByDesc("create_time");
        return wrapper;
    }

    private void ensureCodeUnique(IService service, String code, Long id) {
        QueryWrapper<ErpMasterData> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code).last("limit 1");
        ErpMasterData exists = (ErpMasterData) service.getOne(wrapper);
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "编码已存在");
        }
    }

    private void ensureNotReferenced(String type, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        for (Long id : ids) {
            switch (type) {
                case "product-category" -> {
                    ensureNoProductRef("category_id", id, "商品分类已被商品引用，不能删除，请停用");
                    ensureNoMasterChild(productCategoryService, id, "商品分类存在下级分类，不能删除");
                }
                case "unit" -> {
                    ensureNoProductRef("unit_id", id, "单位已被商品引用，不能删除，请停用");
                    ensureNoBillItemRef("unit_id", id, "单位已被单据明细引用，不能删除，请停用");
                    ensureNoStockCheckItemRef("unit_id", id, "单位已被盘点明细引用，不能删除，请停用");
                }
                case "product-brand" -> ensureNoProductRef("brand_id", id, "商品品牌已被商品引用，不能删除，请停用");
                case "product-attribute" -> {
                    ensureNoMasterChild(productAttributeService, id, "属性存在下级节点，不能删除");
                    ensureNoFindSetProductRef("attribute_ids", id, "属性已被商品引用，不能删除，请停用");
                    ensureNoFindSetCategoryRef("attribute_ids", id, "属性已被商品分类引用，不能删除，请停用");
                    ensureNoFindSetBillItemRef("option_attribute_ids", id, "属性已被单据明细引用，不能删除，请停用");
                }
                case "customer" -> ensureNoPartnerRef(id, "CUSTOMER", "客户已被单据、财务单据或往来流水引用，不能删除，请停用");
                case "supplier" -> ensureNoPartnerRef(id, "SUPPLIER", "供应商已被单据、财务单据或往来流水引用，不能删除，请停用");
                case "warehouse" -> ensureNoWarehouseRef(id);
                case "account" -> ensureNoAccountRef(id);
                case "agent-level" -> ensureNoCustomerLevelRef(id);
                default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的基础资料类型");
            }
        }
    }

    private void ensureNoProductRef(String column, Long id, String message) {
        if (productService.count(new QueryWrapper<ErpProduct>().eq(column, id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void ensureNoBillItemRef(String column, Long id, String message) {
        if (billItemService.count(new QueryWrapper<ErpBillItem>().eq(column, id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void ensureNoFindSetProductRef(String column, Long id, String message) {
        if (productService.count(new QueryWrapper<ErpProduct>().apply("FIND_IN_SET({0}, " + column + ")", id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void ensureNoFindSetCategoryRef(String column, Long id, String message) {
        if (productCategoryService.count(new QueryWrapper<ErpProductCategory>().apply("FIND_IN_SET({0}, " + column + ")", id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void ensureNoFindSetBillItemRef(String column, Long id, String message) {
        if (billItemService.count(new QueryWrapper<ErpBillItem>().apply("FIND_IN_SET({0}, " + column + ")", id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void ensureNoStockCheckItemRef(String column, Long id, String message) {
        if (stockCheckItemService.count(new QueryWrapper<ErpStockCheckItem>().eq(column, id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void ensureNoMasterChild(IService service, Long id, String message) {
        if (service.count(new QueryWrapper<ErpMasterData>().eq("parent_id", id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void ensureNoPartnerRef(Long id, String partnerType, String message) {
        boolean referenced = billService.count(new QueryWrapper<ErpBill>().eq("partner_type", partnerType).eq("partner_id", id)) > 0
                || financeBillService.count(new QueryWrapper<ErpFinanceBill>().eq("partner_type", partnerType).eq("partner_id", id)) > 0
                || partnerFlowService.count(new QueryWrapper<ErpPartnerFlow>().eq("partner_type", partnerType).eq("partner_id", id)) > 0;
        if (referenced) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void ensureNoWarehouseRef(Long id) {
        boolean referenced = billService.count(new QueryWrapper<ErpBill>().eq("warehouse_id", id)) > 0
                || billItemService.count(new QueryWrapper<ErpBillItem>().eq("warehouse_id", id)) > 0
                || stockBalanceService.count(new QueryWrapper<ErpStockBalance>().eq("warehouse_id", id)) > 0
                || stockFlowService.count(new QueryWrapper<ErpStockFlow>().eq("warehouse_id", id)) > 0
                || stockCheckService.count(new QueryWrapper<ErpStockCheck>().eq("warehouse_id", id)) > 0
                || stockCheckItemService.count(new QueryWrapper<ErpStockCheckItem>().eq("warehouse_id", id)) > 0;
        if (referenced) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仓库已被单据、库存或盘点引用，不能删除，请停用");
        }
    }

    private void ensureNoAccountRef(Long id) {
        boolean referenced = billService.count(new QueryWrapper<ErpBill>().eq("account_id", id)) > 0
                || financeBillService.count(new QueryWrapper<ErpFinanceBill>().eq("account_id", id)) > 0
                || fundFlowService.count(new QueryWrapper<ErpFundFlow>().eq("account_id", id)) > 0;
        if (referenced) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户已被单据、财务单据或资金流水引用，不能删除，请停用");
        }
    }

    private void ensureNoCustomerLevelRef(Long id) {
        if (customerService.count(new QueryWrapper<ErpCustomer>().eq("level_id", id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "代理等级已被客户引用，不能删除，请停用");
        }
    }

    private ErpMasterData toEntity(String type, ErpMasterDataSaveBO bo) {
        ErpMasterData entity = switch (type) {
            case "product-category" -> BeanUtil.copyProperties(bo, ErpProductCategory.class);
            case "unit" -> BeanUtil.copyProperties(bo, ErpUnit.class);
            case "product-brand" -> BeanUtil.copyProperties(bo, ErpProductBrand.class);
            case "product-attribute" -> BeanUtil.copyProperties(bo, ErpProductAttribute.class);
            case "customer" -> BeanUtil.copyProperties(bo, ErpCustomer.class);
            case "supplier" -> BeanUtil.copyProperties(bo, ErpSupplier.class);
            case "warehouse" -> BeanUtil.copyProperties(bo, ErpWarehouse.class);
            case "account" -> BeanUtil.copyProperties(bo, ErpAccount.class);
            case "agent-level" -> BeanUtil.copyProperties(bo, ErpAgentLevel.class);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的基础资料类型");
        };
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getParentId() == null) {
            entity.setParentId(0L);
        }
        if (entity instanceof ErpProductAttribute attribute) {
            BigDecimal extraAmount = bo.getExtraAmount() == null ? BigDecimal.ZERO : bo.getExtraAmount();
            if (extraAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "额外加钱不能小于0");
            }
            attribute.setExtraAmount(extraAmount);
        }
        return entity;
    }

    private void ensureValidParent(String type, Long parentId, Long id) {
        if (id != null && parentId != null && id.equals(parentId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "上级节点不能选择自己");
        }
        if ("product-attribute".equals(type)) {
            if (parentId == null || parentId == 0L) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择属性组");
            }
            ErpProductAttribute parent = productAttributeService.getById(parentId);
            if (parent == null || !Long.valueOf(0L).equals(parent.getParentId())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品属性只允许维护属性组下的平铺选项");
            }
        }
    }

    private IService<? extends ErpMasterData> service(String type) {
        return switch (type) {
            case "product-category" -> productCategoryService;
            case "unit" -> unitService;
            case "product-brand" -> productBrandService;
            case "product-attribute" -> productAttributeService;
            case "customer" -> customerService;
            case "supplier" -> supplierService;
            case "warehouse" -> warehouseService;
            case "account" -> accountService;
            case "agent-level" -> agentLevelService;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的基础资料类型");
        };
    }

    private String permission(String type, String action) {
        return "erp:" + type + ":" + action;
    }
}
