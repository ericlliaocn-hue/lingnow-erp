package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpProductQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpProductSaveBO;
import cc.lingnow.admin.model.vo.erp.ErpProductVO;
import cc.lingnow.admin.util.CsvExportUtil;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.*;
import cc.lingnow.biz.erp.service.*;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Tag(name = "ERP商品")
@RestController
@RequestMapping("/erp/product")
@RequiredArgsConstructor
public class ErpProductController {

    private static final String COST_PRICE_EDIT_PERMISSION = "erp:product:cost:edit";
    private static final String SEARCH_TOKEN_SEPARATOR = "[\\s,，、;；|/]+";
    private static final String NORMALIZED_CODE_SEARCH_SQL = normalizedColumnSql("code");
    private static final String NORMALIZED_NAME_SEARCH_SQL = normalizedColumnSql("name");
    private static final String NORMALIZED_BARCODE_SEARCH_SQL = normalizedColumnSql("barcode");
    private static final String NORMALIZED_SPEC_SEARCH_SQL = normalizedColumnSql("spec");
    private static final String NORMALIZED_ATTRIBUTE_SEARCH_SQL = normalizedColumnSql("attribute_text");
    private static final String NORMALIZED_PRODUCT_SEARCH_SQL = "CONCAT_WS('', " +
            NORMALIZED_CODE_SEARCH_SQL + ", " +
            NORMALIZED_NAME_SEARCH_SQL + ", " +
            NORMALIZED_BARCODE_SEARCH_SQL + ", " +
            NORMALIZED_SPEC_SEARCH_SQL + ", " +
            NORMALIZED_ATTRIBUTE_SEARCH_SQL + ")";

    private final ErpProductService productService;
    private final ErpProductCategoryService categoryService;
    private final ErpProductBrandService brandService;
    private final ErpUnitService unitService;
    private final ErpProductAttributeService attributeService;
    private final ErpBillItemService billItemService;
    private final ErpStockBalanceService stockBalanceService;

    @GetMapping("/list")
    public Result<PageResult<ErpProductVO>> list(ErpProductQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:product:list");
        QueryWrapper<ErpProduct> wrapper = applyProductSort(wrapper(query), query.getKeyword(), null);
        IPage<ErpProduct> page = productService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ErpProductVO> records = page.getRecords().stream().map(this::toVO).toList();
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @GetMapping("/options")
    public Result<List<ErpProductVO>> options(ErpProductQueryBO query) {
        StpAdminUtil.stpLogic.checkPermission("erp:product:options");
        QueryWrapper<ErpProduct> wrapper = applyProductSort(wrapper(query).eq("status", 1), query.getKeyword(), 50);
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
    @Log(title = "ERP商品", businessType = BusinessType.INSERT)
    public Result<Void> add(@Valid @RequestBody ErpProductSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:product:add");
        bo.setCode(StrUtil.isBlank(bo.getCode()) ? nextProductCode() : bo.getCode());
        applyCostPriceOnAdd(bo);
        ensureCodeUnique(bo.getCode(), null);
        productService.save(toEntity(bo));
        return Result.success();
    }

    @PutMapping
    @Log(title = "ERP商品", businessType = BusinessType.UPDATE)
    public Result<Void> edit(@Valid @RequestBody ErpProductSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        StpAdminUtil.stpLogic.checkPermission("erp:product:edit");
        ErpProduct old = productService.getById(bo.getId());
        if (old == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        bo.setCode(StrUtil.isBlank(bo.getCode()) ? old.getCode() : bo.getCode());
        applyCostPriceOnEdit(bo, old);
        ensureCodeUnique(bo.getCode(), bo.getId());
        productService.updateById(toEntity(bo));
        return Result.success();
    }

    @DeleteMapping("/{ids}")
    @Log(title = "ERP商品", businessType = BusinessType.DELETE)
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

    @GetMapping("/export")
    @Log(title = "ERP商品", businessType = BusinessType.EXPORT, isSaveResponseData = false)
    public void export(ErpProductQueryBO query, HttpServletResponse response) throws Exception {
        StpAdminUtil.stpLogic.checkPermission("erp:product:export");
        List<List<String>> rows = productService.list(applyProductSort(wrapper(query), query.getKeyword(), null)).stream()
                .map(this::toVO)
                .map(item -> List.of(
                        text(item.getCode()),
                        text(item.getName()),
                        text(item.getSpec()),
                        text(item.getCategoryName()),
                        text(item.getBrandName()),
                        text(item.getUnitName()),
                        text(item.getAttributeText()),
                        text(item.getImageUrl()),
                        text(item.getBarcode()),
                        text(item.getLocation()),
                        money(item.getPurchasePrice()),
                        money(item.getSalePrice()),
                        money(item.getRetailPrice()),
                        money(item.getMinStock()),
                        money(item.getMaxStock()),
                        Integer.valueOf(1).equals(item.getStatus()) ? "启用" : "停用",
                        text(item.getRemark())
                )).toList();
        CsvExportUtil.write(response, "商品管理.csv",
                List.of("商品编号", "商品名称", "规格", "分类", "品牌", "单位", "辅助属性", "商品图片", "条码", "货位", "成本价", "销售价", "零售价", "最低库存", "最高库存", "状态", "备注"),
                rows);
    }

    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws Exception {
        StpAdminUtil.stpLogic.checkPermission("erp:product:import");
        CsvExportUtil.write(response, "商品导入模板.csv",
                List.of("商品编号", "商品名称", "规格", "分类", "品牌", "单位", "辅助属性", "商品图片", "条码", "货位", "成本价", "销售价", "零售价", "最低库存", "最高库存", "状态", "备注"),
                List.of());
    }

    @PostMapping("/import")
    @Log(title = "ERP商品", businessType = BusinessType.IMPORT)
    public Result<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) throws Exception {
        StpAdminUtil.stpLogic.checkPermission("erp:product:import");
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "导入文件不能为空");
        }
        boolean canEditCostPrice = canEditCostPrice();
        List<String> errors = new ArrayList<>();
        int success = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "导入文件不能为空");
            }
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    ErpProductSaveBO bo = parseProduct(line);
                    bo.setCode(StrUtil.isBlank(bo.getCode()) ? nextProductCode() : bo.getCode());
                    applyCostPriceOnAdd(bo, canEditCostPrice);
                    ensureCodeUnique(bo.getCode(), null);
                    productService.save(toEntity(bo));
                    success++;
                } catch (Exception ex) {
                    errors.add("第" + lineNo + "行：" + ex.getMessage());
                }
            }
        }
        return Result.success(Map.of("success", success, "fail", errors.size(), "errors", errors));
    }

    private QueryWrapper<ErpProduct> wrapper(ErpProductQueryBO query) {
        QueryWrapper<ErpProduct> wrapper = new QueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getCode()), "code", query.getCode())
                .like(StrUtil.isNotBlank(query.getName()), "name", query.getName())
                .like(StrUtil.isNotBlank(query.getBarcode()), "barcode", query.getBarcode())
                .eq(query.getCategoryId() != null, "category_id", query.getCategoryId())
                .eq(query.getBrandId() != null, "brand_id", query.getBrandId())
                .eq(query.getStatus() != null, "status", query.getStatus());
        if (StrUtil.isNotBlank(query.getKeyword())) {
            for (String token : searchTokens(query.getKeyword())) {
                String normalizedToken = normalizeSearchToken(token);
                wrapper.and(item -> {
                    item.like("code", token)
                            .or()
                            .like("name", token)
                            .or()
                            .like("barcode", token)
                            .or()
                            .like("spec", token)
                            .or()
                            .like("attribute_text", token);
                    if (StrUtil.isNotBlank(normalizedToken)) {
                        item.or().apply(NORMALIZED_PRODUCT_SEARCH_SQL + " LIKE CONCAT('%', {0}, '%')", normalizedToken);
                    }
                });
            }
        }
        for (String attributeId : splitIds(query.getAttributeIds())) {
            wrapper.apply("FIND_IN_SET({0}, attribute_ids)", attributeId);
        }
        return wrapper;
    }

    private QueryWrapper<ErpProduct> applyProductSort(QueryWrapper<ErpProduct> wrapper, String keyword, Integer limit) {
        if (StrUtil.isBlank(keyword)) {
            wrapper.orderByAsc("sort_order").orderByDesc("create_time");
            if (limit != null) {
                wrapper.last("limit " + limit);
            }
            return wrapper;
        }
        String orderSql = "ORDER BY " + productRelevanceSql(keyword) + ", sort_order ASC, create_time DESC";
        wrapper.last(limit == null ? orderSql : orderSql + " LIMIT " + limit);
        return wrapper;
    }

    private String productRelevanceSql(String keyword) {
        List<String> tokens = searchTokens(keyword);
        List<String> normalizedTokens = tokens.stream()
                .map(this::normalizeSearchToken)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        String keywordText = keyword.trim();
        String normalizedKeyword = normalizeSearchToken(keywordText);
        String exact = escapeSqlLiteral(keywordText);
        String prefixLike = escapeSqlLiteral(escapeSqlLike(keywordText) + "%");
        String containsLike = escapeSqlLiteral("%" + escapeSqlLike(keywordText) + "%");
        String normalizedExact = escapeSqlLiteral(normalizedKeyword);
        String normalizedPrefixLike = escapeSqlLiteral(escapeSqlLike(normalizedKeyword) + "%");
        String normalizedContainsLike = escapeSqlLiteral("%" + escapeSqlLike(normalizedKeyword) + "%");
        List<String> sortItems = new ArrayList<>();
        sortItems.add("CASE " +
                "WHEN code = '" + exact + "' OR barcode = '" + exact + "' THEN 0 " +
                (StrUtil.isNotBlank(normalizedKeyword) ? "WHEN " + NORMALIZED_CODE_SEARCH_SQL + " = '" + normalizedExact + "' OR " + NORMALIZED_BARCODE_SEARCH_SQL + " = '" + normalizedExact + "' THEN 0 " : "") +
                "WHEN name = '" + exact + "' THEN 1 " +
                (StrUtil.isNotBlank(normalizedKeyword) ? "WHEN " + NORMALIZED_NAME_SEARCH_SQL + " = '" + normalizedExact + "' THEN 1 " : "") +
                "WHEN name LIKE '" + prefixLike + "' ESCAPE '/' THEN 2 " +
                (StrUtil.isNotBlank(normalizedKeyword) ? "WHEN " + NORMALIZED_NAME_SEARCH_SQL + " LIKE '" + normalizedPrefixLike + "' ESCAPE '/' THEN 2 " : "") +
                "WHEN name LIKE '" + containsLike + "' ESCAPE '/' THEN 3 " +
                (StrUtil.isNotBlank(normalizedKeyword) ? "WHEN " + NORMALIZED_PRODUCT_SEARCH_SQL + " LIKE '" + normalizedContainsLike + "' ESCAPE '/' THEN 4 " : "") +
                "WHEN spec LIKE '" + containsLike + "' ESCAPE '/' OR attribute_text LIKE '" + containsLike + "' ESCAPE '/' THEN 5 " +
                "WHEN code LIKE '" + containsLike + "' ESCAPE '/' OR barcode LIKE '" + containsLike + "' ESCAPE '/' THEN 6 " +
                "ELSE 9 END");
        if (!normalizedTokens.isEmpty()) {
            sortItems.add(allTokensInFieldSql(NORMALIZED_NAME_SEARCH_SQL, normalizedTokens));
            sortItems.add(tokenScoreSql(NORMALIZED_NAME_SEARCH_SQL, normalizedTokens, 10) + " DESC");
            sortItems.add(tokenScoreSql(NORMALIZED_CODE_SEARCH_SQL, normalizedTokens, 8) + " DESC");
            sortItems.add(tokenScoreSql(NORMALIZED_BARCODE_SEARCH_SQL, normalizedTokens, 8) + " DESC");
            sortItems.add(tokenScoreSql(NORMALIZED_SPEC_SEARCH_SQL, normalizedTokens, 4) + " DESC");
            sortItems.add(tokenScoreSql(NORMALIZED_ATTRIBUTE_SEARCH_SQL, normalizedTokens, 2) + " DESC");
        }
        return String.join(", ", sortItems);
    }

    private String allTokensInFieldSql(String fieldSql, List<String> normalizedTokens) {
        String condition = normalizedTokens.stream()
                .map(token -> fieldSql + " LIKE '" + normalizedLike(token) + "' ESCAPE '/'")
                .reduce((left, right) -> left + " AND " + right)
                .orElse("1 = 1");
        return "CASE WHEN " + condition + " THEN 0 ELSE 1 END";
    }

    private String tokenScoreSql(String fieldSql, List<String> normalizedTokens, int weight) {
        return "(" + normalizedTokens.stream()
                .map(token -> "CASE WHEN " + fieldSql + " LIKE '" + normalizedLike(token) + "' ESCAPE '/' THEN " + weight + " ELSE 0 END")
                .reduce((left, right) -> left + " + " + right)
                .orElse("0") + ")";
    }

    private String normalizedLike(String token) {
        return "%" + escapeSqlLiteral(escapeSqlLike(token)) + "%";
    }

    private static String normalizedColumnSql(String column) {
        return "REGEXP_REPLACE(LOWER(IFNULL(" + column + ", '')), '[^0-9a-z一-龥]+', '')";
    }

    private List<String> searchTokens(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }
        return Arrays.stream(keyword.trim().split(SEARCH_TOKEN_SEPARATOR))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private String normalizeSearchToken(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^0-9a-z\\u4e00-\\u9fa5]+", "");
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String escapeSqlLike(String value) {
        return value == null ? "" : value
                .replace("/", "//")
                .replace("%", "/%")
                .replace("_", "/_");
    }

    private void ensureCodeUnique(String code, Long id) {
        ErpProduct exists = productService.getOne(new QueryWrapper<ErpProduct>().eq("code", code).last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品编号已存在");
        }
    }

    private String nextProductCode() {
        String prefix = "SP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long serial = productService.count(new QueryWrapper<ErpProduct>().likeRight("code", prefix)) + 1;
        String code;
        do {
            code = prefix + String.format("%04d", serial++);
        } while (productService.count(new QueryWrapper<ErpProduct>().eq("code", code)) > 0);
        return code;
    }

    private void applyCostPriceOnAdd(ErpProductSaveBO bo) {
        applyCostPriceOnAdd(bo, canEditCostPrice());
    }

    private void applyCostPriceOnAdd(ErpProductSaveBO bo, boolean canEditCostPrice) {
        if (canEditCostPrice) {
            return;
        }
        bo.setPurchasePrice(BigDecimal.ZERO);
    }

    private void applyCostPriceOnEdit(ErpProductSaveBO bo, ErpProduct old) {
        if (canEditCostPrice()) {
            return;
        }
        bo.setPurchasePrice(old.getPurchasePrice());
    }

    private boolean canEditCostPrice() {
        List<String> permissions = StpAdminUtil.stpLogic.getPermissionList();
        return permissions.contains("*:*:*") || permissions.contains(COST_PRICE_EDIT_PERMISSION);
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
        product.setAttributeText(attributeText(product.getAttributeIds(), product.getAttributeText()));
        return product;
    }

    private ErpProductVO toVO(ErpProduct product) {
        ErpProductVO vo = BeanUtil.copyProperties(product, ErpProductVO.class);
        vo.setCategoryName(masterName(categoryService.getById(product.getCategoryId())));
        vo.setBrandName(masterName(brandService.getById(product.getBrandId())));
        vo.setUnitName(masterName(unitService.getById(product.getUnitId())));
        vo.setAttributeText(attributeText(product.getAttributeIds(), product.getAttributeText()));
        return vo;
    }

    private ErpProductSaveBO parseProduct(String line) {
        List<String> values = parseCsvLine(line);
        String code = value(values, 0);
        String name = value(values, 1);
        if (StrUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品名称不能为空");
        }
        ErpProductSaveBO bo = new ErpProductSaveBO();
        bo.setCode(code);
        bo.setName(name);
        bo.setSpec(value(values, 2));
        bo.setCategoryId(categoryId(value(values, 3)));
        bo.setBrandId(brandId(value(values, 4)));
        bo.setUnitId(unitId(value(values, 5)));
        bo.setAttributeText(value(values, 6));
        bo.setImageUrl(value(values, 7));
        bo.setBarcode(value(values, 8));
        bo.setLocation(value(values, 9));
        bo.setPurchasePrice(decimal(value(values, 10), "成本价"));
        bo.setSalePrice(decimal(value(values, 11), "销售价"));
        bo.setRetailPrice(decimal(value(values, 12), "零售价"));
        bo.setMinStock(decimal(value(values, 13), "最低库存"));
        bo.setMaxStock(decimal(value(values, 14), "最高库存"));
        bo.setStatus(status(value(values, 15)));
        bo.setRemark(value(values, 16));
        return bo;
    }

    private String attributeText(String attributeIds, String fallback) {
        if (StrUtil.isBlank(attributeIds)) {
            return fallback;
        }
        List<Long> ids = Arrays.stream(attributeIds.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return fallback;
        }
        List<String> names = attributeService.listByIds(ids).stream()
                .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                .map(ErpProductAttribute::getName)
                .filter(StrUtil::isNotBlank)
                .toList();
        return names.isEmpty() ? fallback : String.join(" / ", names);
    }

    private List<String> splitIds(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quote = !quote;
                }
            } else if (ch == ',' && !quote) {
                values.add(current.toString().trim().replace("\uFEFF", ""));
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim().replace("\uFEFF", ""));
        return values;
    }

    private Long categoryId(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        ErpMasterData data = categoryService.getOne(new QueryWrapper<ErpProductCategory>().eq("name", name).last("limit 1"));
        if (data == null || !Integer.valueOf(1).equals(data.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分类不存在或已停用：" + name);
        }
        return data.getId();
    }

    private Long brandId(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        ErpMasterData data = brandService.getOne(new QueryWrapper<ErpProductBrand>().eq("name", name).last("limit 1"));
        if (data == null || !Integer.valueOf(1).equals(data.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "品牌不存在或已停用：" + name);
        }
        return data.getId();
    }

    private Long unitId(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        ErpMasterData data = unitService.getOne(new QueryWrapper<ErpUnit>().eq("name", name).last("limit 1"));
        if (data == null || !Integer.valueOf(1).equals(data.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单位不存在或已停用：" + name);
        }
        return data.getId();
    }

    private BigDecimal decimal(String value, String label) {
        if (StrUtil.isBlank(value)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal decimal = new BigDecimal(value);
            if (decimal.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }
            return decimal;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "必须为非负数字");
        }
    }

    private Integer status(String value) {
        if (StrUtil.isBlank(value) || "启用".equals(value) || "1".equals(value)) {
            return 1;
        }
        if ("停用".equals(value) || "0".equals(value)) {
            return 0;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "状态只能是启用/停用");
    }

    private String value(List<String> values, int index) {
        return index < values.size() ? values.get(index) : "";
    }

    private String money(BigDecimal value) {
        return nvl(value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String masterName(ErpMasterData data) {
        return data == null ? null : data.getName();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
