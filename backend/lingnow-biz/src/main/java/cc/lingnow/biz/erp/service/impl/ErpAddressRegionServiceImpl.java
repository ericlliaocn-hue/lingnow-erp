package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.model.ErpAddressRegionVO;
import cc.lingnow.biz.erp.service.ErpAddressRegionService;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpAddressRegionServiceImpl implements ErpAddressRegionService {

    private static final String ROOT_PARENT_CODE = "0";
    private static final String CACHE_PREFIX = "erp:address-region:v2:children:";
    private static final String SEARCH_CACHE_PREFIX = "erp:address-region:v2:search:";
    private static final String PCAS_DATA_PATH = "erp/address/pcas-code.json";
    private static final String VILLAGE_DATA_PATH = "erp/address/villages.json.gz";
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final Set<String> GENERIC_REGION_NAMES = Set.of("市辖区", "县");

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private volatile RegionStore regionStore;

    @Override
    public List<ErpAddressRegionVO> listChildren(String parentCode) {
        String normalizedParentCode = normalizeParentCode(parentCode);
        String cacheKey = CACHE_PREFIX + normalizedParentCode;
        List<ErpAddressRegionVO> cached = readCachedList(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<ErpAddressRegionVO> children = getRegionStore().childrenIndex().getOrDefault(normalizedParentCode, List.of());
        redisTemplate.opsForValue().set(cacheKey, new ArrayList<>(children));
        return children;
    }

    @Override
    public List<ErpAddressRegionVO> search(String keyword, int limit) {
        String normalizedKeyword = normalizeSearchText(keyword);
        if (StrUtil.isBlank(normalizedKeyword)) {
            return List.of();
        }
        int safeLimit = limit <= 0 ? DEFAULT_SEARCH_LIMIT : Math.min(limit, 50);
        String cacheKey = SEARCH_CACHE_PREFIX + safeLimit + ":" + normalizedKeyword;
        List<ErpAddressRegionVO> cached = readCachedList(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<RegionScore> scores = new ArrayList<>();
        for (ErpAddressRegionVO region : getRegionStore().searchableRegions()) {
            int score = scoreRegion(region, normalizedKeyword);
            if (score >= 0) {
                scores.add(new RegionScore(region, score));
            }
        }
        scores.sort(Comparator
                .comparingInt(RegionScore::score).reversed()
                .thenComparing((RegionScore score) -> score.region().getLevel(), Comparator.reverseOrder())
                .thenComparing(score -> String.join("", score.region().getPathNames())));
        List<ErpAddressRegionVO> result = scores.stream()
                .limit(safeLimit)
                .map(RegionScore::region)
                .toList();
        redisTemplate.opsForValue().set(cacheKey, new ArrayList<>(result));
        return result;
    }

    @Override
    public ErpAddressRegionVO matchAddress(String text) {
        String normalizedText = normalizeSearchText(text);
        if (StrUtil.isBlank(normalizedText)) {
            return null;
        }
        for (ErpAddressRegionVO region : getRegionStore().matchRegions()) {
            if (pathMatches(region, normalizedText)) {
                return region;
            }
        }
        return null;
    }

    private String normalizeParentCode(String parentCode) {
        return StrUtil.blankToDefault(parentCode, ROOT_PARENT_CODE).trim();
    }

    private List<ErpAddressRegionVO> readCachedList(String cacheKey) {
        Object value = redisTemplate.opsForValue().get(cacheKey);
        if (!(value instanceof List<?> list)) {
            return null;
        }
        List<ErpAddressRegionVO> result = new ArrayList<>(list.size());
        for (Object item : list) {
            ErpAddressRegionVO vo = convertRegion(item);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    private ErpAddressRegionVO convertRegion(Object item) {
        if (item instanceof ErpAddressRegionVO vo) {
            return vo;
        }
        if (item instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, ErpAddressRegionVO.class);
        }
        return null;
    }

    private RegionStore getRegionStore() {
        RegionStore local = regionStore;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (regionStore == null) {
                regionStore = loadRegionStore();
            }
            return regionStore;
        }
    }

    private RegionStore loadRegionStore() {
        long start = System.currentTimeMillis();
        Map<String, List<ErpAddressRegionVO>> childrenIndex = new LinkedHashMap<>();
        Map<String, ErpAddressRegionVO> regionIndex = new LinkedHashMap<>();
        try (InputStream inputStream = new ClassPathResource(PCAS_DATA_PATH).getInputStream()) {
            List<AddressRegionNode> rootNodes = objectMapper.readValue(inputStream, new TypeReference<List<AddressRegionNode>>() {});
            collectPcasChildren(childrenIndex, regionIndex, ROOT_PARENT_CODE, rootNodes, new ArrayList<>(), new ArrayList<>());
            loadVillageChildren(childrenIndex, regionIndex);
        } catch (Exception e) {
            log.error("加载行政区划数据失败", e);
            return new RegionStore(Map.of(ROOT_PARENT_CODE, List.of()), Map.of(), List.of(), List.of());
        }

        Map<String, List<ErpAddressRegionVO>> immutableChildren = new LinkedHashMap<>();
        childrenIndex.forEach((code, children) -> immutableChildren.put(code, List.copyOf(children)));
        List<ErpAddressRegionVO> searchableRegions = new ArrayList<>(regionIndex.values());
        searchableRegions.sort(Comparator
                .comparing((ErpAddressRegionVO region) -> region.getLevel() == null ? 0 : region.getLevel(), Comparator.reverseOrder())
                .thenComparing(region -> String.join("", region.getPathNames())));
        log.info("行政区划数据加载完成: regions={}, parents={}, cost={}ms", regionIndex.size(), immutableChildren.size(), System.currentTimeMillis() - start);
        return new RegionStore(Map.copyOf(immutableChildren), Map.copyOf(regionIndex), List.copyOf(searchableRegions), List.copyOf(searchableRegions));
    }

    private void collectPcasChildren(Map<String, List<ErpAddressRegionVO>> childrenIndex,
                                     Map<String, ErpAddressRegionVO> regionIndex,
                                     String parentCode,
                                     List<AddressRegionNode> nodes,
                                     List<String> parentPath,
                                     List<String> parentPathNames) {
        if (nodes == null || nodes.isEmpty()) {
            childrenIndex.putIfAbsent(parentCode, new ArrayList<>());
            return;
        }

        List<ErpAddressRegionVO> children = childrenIndex.computeIfAbsent(parentCode, key -> new ArrayList<>());
        for (AddressRegionNode node : nodes) {
            List<String> path = append(parentPath, node.getCode());
            List<String> pathNames = append(parentPathNames, node.getName());
            ErpAddressRegionVO region = new ErpAddressRegionVO(
                    node.getCode(),
                    node.getName(),
                    levelOf(node.getCode()),
                    node.getChildren() == null || node.getChildren().isEmpty(),
                    path,
                    pathNames
            );
            children.add(region);
            regionIndex.put(region.getCode(), region);
            collectPcasChildren(childrenIndex, regionIndex, node.getCode(), node.getChildren(), path, pathNames);
        }
    }

    private void loadVillageChildren(Map<String, List<ErpAddressRegionVO>> childrenIndex,
                                     Map<String, ErpAddressRegionVO> regionIndex) {
        ClassPathResource resource = new ClassPathResource(VILLAGE_DATA_PATH);
        if (!resource.exists()) {
            log.warn("村级行政区划数据不存在: {}", VILLAGE_DATA_PATH);
            return;
        }
        int loaded = 0;
        try (InputStream rawInputStream = resource.getInputStream();
             GZIPInputStream gzipInputStream = new GZIPInputStream(rawInputStream);
             JsonParser parser = objectMapper.getFactory().createParser(gzipInputStream)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                log.warn("村级行政区划数据格式不正确: {}", VILLAGE_DATA_PATH);
                return;
            }
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                VillageRegionNode village = parser.readValueAs(VillageRegionNode.class);
                ErpAddressRegionVO street = regionIndex.get(village.getStreetCode());
                if (street == null || StrUtil.isBlank(village.getCode()) || StrUtil.isBlank(village.getName())) {
                    continue;
                }
                street.setLeaf(false);
                List<String> path = append(street.getPath(), village.getCode());
                List<String> pathNames = append(street.getPathNames(), village.getName());
                ErpAddressRegionVO region = new ErpAddressRegionVO(village.getCode(), village.getName(), 5, true, path, pathNames);
                childrenIndex.computeIfAbsent(village.getStreetCode(), key -> new ArrayList<>()).add(region);
                childrenIndex.putIfAbsent(village.getCode(), new ArrayList<>());
                regionIndex.put(region.getCode(), region);
                loaded++;
            }
            log.info("村级行政区划数据加载完成: {}", loaded);
        } catch (Exception e) {
            log.error("加载村级行政区划数据失败: {}", VILLAGE_DATA_PATH, e);
        }
    }

    private List<String> append(List<String> source, String value) {
        List<String> next = new ArrayList<>(source == null ? List.of() : source);
        next.add(value);
        return next;
    }

    private int scoreRegion(ErpAddressRegionVO region, String keyword) {
        String name = normalizeSearchText(region.getName());
        String pathText = normalizeSearchText(String.join("", region.getPathNames()));
        if (StrUtil.isBlank(name)) {
            return -1;
        }
        int level = region.getLevel() == null ? 0 : region.getLevel();
        if (name.equals(keyword)) {
            return level <= 4 ? 140 : 120;
        }
        if (regionAliases(region.getName()).contains(keyword)) {
            return level <= 4 ? 135 : 115;
        }
        if (name.startsWith(keyword)) {
            return level <= 4 ? 125 : 110;
        }
        if (name.contains(keyword)) {
            return level <= 4 ? 115 : 100;
        }
        if (pathText.contains(keyword)) {
            return 80 + Math.min(level, 5);
        }
        for (String alias : regionAliases(region.getName())) {
            if (alias.contains(keyword) || keyword.contains(alias)) {
                return 90;
            }
        }
        return -1;
    }

    private boolean pathMatches(ErpAddressRegionVO region, String text) {
        List<String> names = region.getPathNames();
        if (names == null || names.isEmpty()) {
            return false;
        }
        String leafName = names.get(names.size() - 1);
        if (!nameMatchesText(leafName, text)) {
            return false;
        }
        for (String name : names) {
            if (GENERIC_REGION_NAMES.contains(name)) {
                continue;
            }
            if (!nameMatchesText(name, text)) {
                return false;
            }
        }
        return true;
    }

    private boolean nameMatchesText(String name, String text) {
        for (String alias : regionAliases(name)) {
            if (StrUtil.isNotBlank(alias) && text.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> regionAliases(String name) {
        String normalized = normalizeSearchText(name);
        if (StrUtil.isBlank(normalized)) {
            return Set.of();
        }
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(normalized);
        addAlias(aliases, normalized, "街道办事处");
        addAlias(aliases, normalized, "街道");
        addAlias(aliases, normalized, "镇");
        addAlias(aliases, normalized, "乡");
        addAlias(aliases, normalized, "苏木");
        addAlias(aliases, normalized, "社区居民委员会");
        addAlias(aliases, normalized, "社区居委会");
        addAlias(aliases, normalized, "居民委员会");
        addAlias(aliases, normalized, "村民委员会");
        addAlias(aliases, normalized, "村委会");
        addAlias(aliases, normalized, "委员会");
        return aliases;
    }

    private void addAlias(Set<String> aliases, String value, String suffix) {
        if (value.endsWith(suffix) && value.length() > suffix.length()) {
            aliases.add(value.substring(0, value.length() - suffix.length()));
        }
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s/／,，;；:：()（）\\[\\]【】]+", "")
                .trim();
    }

    private Integer levelOf(String code) {
        int length = StrUtil.blankToDefault(code, "").length();
        if (length <= 2) return 1;
        if (length <= 4) return 2;
        if (length <= 6) return 3;
        if (length <= 9) return 4;
        return 5;
    }

    @Data
    private static class AddressRegionNode {
        private String code;
        private String name;
        private List<AddressRegionNode> children;
    }

    @Data
    private static class VillageRegionNode {
        private String code;
        private String name;
        private String streetCode;
    }

    private record RegionStore(Map<String, List<ErpAddressRegionVO>> childrenIndex,
                               Map<String, ErpAddressRegionVO> regionIndex,
                               List<ErpAddressRegionVO> searchableRegions,
                               List<ErpAddressRegionVO> matchRegions) {
    }

    private record RegionScore(ErpAddressRegionVO region, int score) {
    }
}
