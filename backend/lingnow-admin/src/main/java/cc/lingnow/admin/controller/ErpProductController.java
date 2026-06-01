package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpProductQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpProductSaveBO;
import cc.lingnow.admin.model.vo.erp.ErpProductVO;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "ERP商品")
@RestController
@RequestMapping("/erp/product")
@RequiredArgsConstructor
public class ErpProductController {

    private final ErpProductService productService;
    private final ErpProductCategoryService categoryService;
    private final ErpProductBrandService brandService;
    private final ErpUnitService unitService;
    private final ErpBillItemService billItemService;
    private final ErpStockBalanceService stockBalanceService;

    @GetMapping("/list")
    public Result<PageResult<ErpProductVO>> list(ErpProductQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:product:list");
        QueryWrapper<ErpProduct> wrapper = wrapper(query).orderByAsc("sort_order").orderByDesc("create_time");
        IPage<ErpProduct> page = productService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ErpProductVO> records = page.getRecords().stream().map(this::toVO).toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @GetMapping("/options")
    public Result<List<ErpProductVO>> options(ErpProductQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:product:options");
        QueryWrapper<ErpProduct> wrapper = wrapper(query)
                .eq("status", 1)
                .orderByAsc("sort_order")
                .orderByDesc("create_time")
                .last("limit 50");
        return Result.success(productService.list(wrapper).stream().map(this::toVO).toList());
    }

    @GetMapping("/{id}")
    public Result<ErpProductVO> getInfo(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:product:list");
        ErpProduct product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return Result.success(toVO(product));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody ErpProductSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:product:add");
        ensureCodeUnique(bo.getCode(), null);
        productService.save(toEntity(bo));
        return Result.success();
    }

    @PutMapping
    public Result<Void> edit(@Valid @RequestBody ErpProductSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        StpAdminUtil.stpLogic.checkPermission("erp:product:edit");
        ensureCodeUnique(bo.getCode(), bo.getId());
        productService.updateById(toEntity(bo));
        return Result.success();
    }

    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable List<Long> ids) {
        StpAdminUtil.stpLogic.checkPermission("erp:product:remove");
        for (Long id : ids) {
            long billRefs = billItemService.count(new QueryWrapper<ErpBillItem>().eq("product_id", id));
            long stockRefs = stockBalanceService.count(new QueryWrapper<ErpStockBalance>().eq("product_id", id).ne("qty", BigDecimal.ZERO));
            if (billRefs > 0 || stockRefs > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品已被单据或库存引用，不能删除，请停用");
            }
        }
        stockBalanceService.remove(new QueryWrapper<ErpStockBalance>().in("product_id", ids).eq("qty", BigDecimal.ZERO));
        productService.removeByIds(ids);
        return Result.success();
    }

    private QueryWrapper<ErpProduct> wrapper(ErpProductQueryBO query) {
        QueryWrapper<ErpProduct> wrapper = new QueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getCode()), "code", query.getCode())
                .like(StrUtil.isNotBlank(query.getName()), "name", query.getName())
                .like(StrUtil.isNotBlank(query.getBarcode()), "barcode", query.getBarcode())
                .eq(query.getCategoryId() != null, "category_id", query.getCategoryId())
                .eq(query.getBrandId() != null, "brand_id", query.getBrandId())
                .eq(query.getStatus() != null, "status", query.getStatus());
        return wrapper;
    }

    private void ensureCodeUnique(String code, Long id) {
        ErpProduct exists = productService.getOne(new QueryWrapper<ErpProduct>().eq("code", code).last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品编号已存在");
        }
    }

    private ErpProduct toEntity(ErpProductSaveBO bo) {
        ErpProduct product = BeanUtil.copyProperties(bo, ErpProduct.class);
        product.setStatus(product.getStatus() == null ? 1 : product.getStatus());
        product.setSortOrder(product.getSortOrder() == null ? 0 : product.getSortOrder());
        product.setPurchasePrice(nvl(product.getPurchasePrice()));
        product.setSalePrice(nvl(product.getSalePrice()));
        product.setRetailPrice(nvl(product.getRetailPrice()));
        product.setMinStock(nvl(product.getMinStock()));
        product.setMaxStock(nvl(product.getMaxStock()));
        return product;
    }

    private ErpProductVO toVO(ErpProduct product) {
        ErpProductVO vo = BeanUtil.copyProperties(product, ErpProductVO.class);
        vo.setCategoryName(masterName(categoryService.getById(product.getCategoryId())));
        vo.setBrandName(masterName(brandService.getById(product.getBrandId())));
        vo.setUnitName(masterName(unitService.getById(product.getUnitId())));
        return vo;
    }

    private String masterName(ErpMasterData data) {
        return data == null ? null : data.getName();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
