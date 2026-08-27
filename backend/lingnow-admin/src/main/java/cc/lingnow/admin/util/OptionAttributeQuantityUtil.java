package cc.lingnow.admin.util;

import cc.lingnow.biz.erp.entity.ErpProductAttribute;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 商品选配项数量工具。主商品数量与每个选配项数量相互独立。
 */
public final class OptionAttributeQuantityUtil {

    private OptionAttributeQuantityUtil() {
    }

    public static LinkedHashMap<String, BigDecimal> normalize(
            Map<String, BigDecimal> requested,
            Collection<String> legacyOptionIds,
            BigDecimal mainQty,
            Set<String> allowedGroupIds,
            Map<String, ErpProductAttribute> attributes) {
        LinkedHashMap<String, BigDecimal> result = new LinkedHashMap<>();
        if (requested == null) {
            BigDecimal legacyQty = positive(mainQty);
            legacyOptionIds.forEach(id -> putAllowed(result, id, legacyQty, allowedGroupIds, attributes));
            return result;
        }
        requested.forEach((id, qty) -> putAllowed(result, id, qty, allowedGroupIds, attributes));
        return result;
    }

    public static LinkedHashMap<String, BigDecimal> parseOrLegacy(
            String quantityJson, Collection<String> legacyOptionIds, BigDecimal mainQty) {
        if (StrUtil.isBlank(quantityJson)) {
            LinkedHashMap<String, BigDecimal> legacy = new LinkedHashMap<>();
            BigDecimal legacyQty = positive(mainQty);
            legacyOptionIds.forEach(id -> legacy.put(id, legacyQty));
            return legacy;
        }
        LinkedHashMap<String, BigDecimal> result = new LinkedHashMap<>();
        try {
            JSONObject json = JSONUtil.parseObj(quantityJson);
            json.forEach((id, value) -> {
                BigDecimal qty = decimal(value);
                if (StrUtil.isNotBlank(id) && qty.compareTo(BigDecimal.ZERO) > 0) {
                    result.put(id, qty);
                }
            });
        } catch (Exception ignored) {
            // 历史异常快照按旧口径恢复，不能把选配数量静默丢失。
            BigDecimal legacyQty = positive(mainQty);
            legacyOptionIds.forEach(id -> result.put(id, legacyQty));
        }
        return result;
    }

    public static String toJson(Map<String, BigDecimal> quantities) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        quantities.forEach((id, qty) -> values.put(id, formatQty(qty)));
        return JSONUtil.toJsonStr(values);
    }

    public static String ids(Map<String, BigDecimal> quantities) {
        return String.join(",", quantities.keySet());
    }

    public static BigDecimal totalExtraAmount(
            Map<String, BigDecimal> quantities, Map<String, ErpProductAttribute> attributes) {
        return quantities.entrySet().stream().map(entry -> {
            ErpProductAttribute attribute = attributes.get(entry.getKey());
            BigDecimal extra = attribute == null || attribute.getExtraAmount() == null
                    ? BigDecimal.ZERO : attribute.getExtraAmount();
            return extra.multiply(entry.getValue());
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal perMainUnit(BigDecimal totalExtraAmount, BigDecimal mainQty) {
        if (mainQty == null || mainQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalExtraAmount.divide(mainQty, 4, RoundingMode.HALF_UP);
    }

    public static String snapshotText(
            Map<String, BigDecimal> quantities, Map<String, ErpProductAttribute> attributes) {
        return quantities.entrySet().stream().map(entry -> {
            ErpProductAttribute option = attributes.get(entry.getKey());
            if (option == null) {
                return "";
            }
            ErpProductAttribute group = attributes.get(String.valueOf(option.getParentId()));
            String groupName = group == null ? "商品属性" : group.getName();
            return groupName + "：" + option.getName() + " × " + formatQty(entry.getValue());
        }).filter(StrUtil::isNotBlank).reduce((left, right) -> left + " / " + right).orElse("");
    }

    private static void putAllowed(
            Map<String, BigDecimal> result,
            String id,
            BigDecimal qty,
            Set<String> allowedGroupIds,
            Map<String, ErpProductAttribute> attributes) {
        ErpProductAttribute option = attributes.get(id);
        BigDecimal normalizedQty = decimal(qty);
        if (option != null
                && allowedGroupIds.contains(String.valueOf(option.getParentId()))
                && normalizedQty.compareTo(BigDecimal.ZERO) > 0) {
            result.put(id, normalizedQty);
        }
    }

    private static BigDecimal positive(BigDecimal value) {
        BigDecimal result = decimal(value);
        return result.compareTo(BigDecimal.ZERO) > 0 ? result : BigDecimal.ONE;
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static String formatQty(BigDecimal value) {
        return decimal(value).stripTrailingZeros().toPlainString();
    }
}
