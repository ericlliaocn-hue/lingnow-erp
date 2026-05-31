package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpMasterDataQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpMasterDataSaveBO;
import cc.lingnow.admin.model.vo.erp.ErpMasterDataVO;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
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
    public Result<Void> add(@PathVariable String type, @Valid @RequestBody ErpMasterDataSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission(permission(type, "add"));
        IService service = service(type);
        ensureCodeUnique(service, bo.getCode(), null);
        service.save(toEntity(type, bo));
        return Result.success();
    }

    @Operation(summary = "ERP基础资料修改")
    @PutMapping("/{type}")
    public Result<Void> edit(@PathVariable String type, @Valid @RequestBody ErpMasterDataSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        StpAdminUtil.stpLogic.checkPermission(permission(type, "edit"));
        IService service = service(type);
        ensureCodeUnique(service, bo.getCode(), bo.getId());
        service.updateById(toEntity(type, bo));
        return Result.success();
    }

    @Operation(summary = "ERP基础资料删除")
    @DeleteMapping("/{type}/{ids}")
    public Result<Void> remove(@PathVariable String type, @PathVariable List<Long> ids) {
        StpAdminUtil.stpLogic.checkPermission(permission(type, "remove"));
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
        return entity;
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
