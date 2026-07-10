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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpAddressRegionServiceImpl implements ErpAddressRegionService, ApplicationRunner {

    private static final String ROOT_PARENT_CODE = "0";
    private static final String CACHE_PREFIX = "erp:address-region:v3:children:";
    private static final String SEARCH_CACHE_PREFIX = "erp:address-region:v3:search:";
    private static final String PCAS_DATA_PATH = "erp/address/pcas-code.json";
    private static final String VILLAGE_DATA_PATH = "erp/address/villages.json.gz";
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int SINGLE_CHAR_SEARCH_MAX_LEVEL = 4;
    private static final int PATH_SEARCH_MIN_LENGTH = 3;
    private static final int CHILDREN_CACHE_WARM_MAX_PARENT_LEVEL = 3;
    private static final List<String> REGION_ALIAS_SUFFIXES = List.of(
            "街道办事处",
            "街道",
            "镇",
            "乡",
            "苏木",
            "社区居民委员会",
            "社区居委会",
            "居民委员会",
            "村民委员会",
            "村委会",
            "委员会"
    );
    private static final Set<String> GENERIC_REGION_NAMES = Set.of("市辖区", "县");

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private volatile RegionStore regionStore;

    @Override
    public void run(ApplicationArguments args) {
        try {
            RegionStore store = getRegionStore();
            warmUpChildrenCache(store);
            log.info("行政区划启动预热完成");
        } catch (Exception e) {
            log.warn("行政区划数据预热失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<ErpAddressRegionVO> listChildren(String parentCode) {
        String normalizedParentCode = normalizeParentCode(parentCode);
        String cacheKey = CACHE_PREFIX + normalizedParentCode;
        List<ErpAddressRegionVO> cached = readCachedList(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<ErpAddressRegionVO> children = getRegionStore().childrenIndex().getOrDefault(normalizedParentCode, List.of());
        cacheList(cacheKey, children);
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

        RegionStore store = getRegionStore();
        List<ErpAddressRegionVO> searchRegions = normalizedKeyword.length() == 1
                ? store.primarySearchRegions()
                : store.searchableRegions();
        PriorityQueue<RegionScore> topScores = new PriorityQueue<>(this::compareRegionScore);
        for (ErpAddressRegionVO region : searchRegions) {
            int score = scoreRegion(region, normalizedKeyword);
            if (score >= 0) {
                RegionScore regionScore = new RegionScore(region, score, String.join("", region.getPathNames()));
                if (topScores.size() < safeLimit) {
                    topScores.offer(regionScore);
                } else if (compareRegionScore(regionScore, topScores.peek()) > 0) {
                    topScores.poll();
                    topScores.offer(regionScore);
                }
            }
        }
        List<RegionScore> scores = new ArrayList<>(topScores);
        scores.sort(Comparator
                .comparingInt(RegionScore::score).reversed()
                .thenComparing((RegionScore score) -> score.region().getLevel(), Comparator.reverseOrder())
                .thenComparing(RegionScore::pathKey));
        List<ErpAddressRegionVO> result = scores.stream()
                .map(RegionScore::region)
                .toList();
        cacheList(cacheKey, result);
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
        Object value;
        try {
            value = redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("行政区划缓存读取失败: key={}, error={}", cacheKey, e.getMessage());
            return null;
        }
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

    private boolean cacheList(String cacheKey, List<ErpAddressRegionVO> list) {
        try {
            redisTemplate.opsForValue().set(cacheKey, new ArrayList<>(list));
            return true;
        } catch (Exception e) {
            log.warn("行政区划缓存写入失败: key={}, error={}", cacheKey, e.getMessage());
            return false;
        }
    }

    private void warmUpChildrenCache(RegionStore store) {
        int cachedKeys = 0;
        for (Map.Entry<String, List<ErpAddressRegionVO>> entry : store.childrenIndex().entrySet()) {
            if (shouldWarmChildrenCache(store, entry.getKey(), entry.getValue())) {
                if (!cacheList(CACHE_PREFIX + entry.getKey(), entry.getValue())) {
                    log.warn("行政区划 Redis 预热中止: cachedKeys={}", cachedKeys);
                    break;
                }
                cachedKeys++;
            }
        }
        log.info("行政区划 Redis 预热完成: childrenKeys={}", cachedKeys);
    }

    private boolean shouldWarmChildrenCache(RegionStore store, String parentCode, List<ErpAddressRegionVO> children) {
        if (children == null || children.isEmpty()) {
            return false;
        }
        if (ROOT_PARENT_CODE.equals(parentCode)) {
            return true;
        }
        ErpAddressRegionVO parent = store.regionIndex().get(parentCode);
        return parent != null && level(parent) <= CHILDREN_CACHE_WARM_MAX_PARENT_LEVEL;
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
            return new RegionStore(Map.of(ROOT_PARENT_CODE, List.of()), Map.of(), List.of(), List.of(), List.of());
        }

        Map<String, List<ErpAddressRegionVO>> immutableChildren = new LinkedHashMap<>();
        childrenIndex.forEach((code, children) -> immutableChildren.put(code, List.copyOf(children)));
        List<ErpAddressRegionVO> searchableRegions = new ArrayList<>(regionIndex.values());
        searchableRegions.sort(Comparator
                .comparing((ErpAddressRegionVO region) -> region.getLevel() == null ? 0 : region.getLevel(), Comparator.reverseOrder())
                .thenComparing(region -> String.join("", region.getPathNames())));
        List<ErpAddressRegionVO> primarySearchRegions = searchableRegions.stream()
                .filter(region -> level(region) <= SINGLE_CHAR_SEARCH_MAX_LEVEL)
                .toList();
        log.info("行政区划数据加载完成: regions={}, parents={}, cost={}ms", regionIndex.size(), immutableChildren.size(), System.currentTimeMillis() - start);
        return new RegionStore(Map.copyOf(immutableChildren), Map.copyOf(regionIndex), List.copyOf(searchableRegions), primarySearchRegions, List.copyOf(searchableRegions));
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
        if (StrUtil.isBlank(name)) {
            return -1;
        }
        int level = level(region);
        if (name.equals(keyword)) {
            return level <= 4 ? 140 : 120;
        }
        if (aliasEquals(name, keyword)) {
            return level <= 4 ? 135 : 115;
        }
        if (name.startsWith(keyword)) {
            return level <= 4 ? 125 : 110;
        }
        if (name.contains(keyword)) {
            return level <= 4 ? 115 : 100;
        }
        if (keyword.length() >= PATH_SEARCH_MIN_LENGTH && level <= 4
                && normalizeSearchText(String.join("", region.getPathNames())).contains(keyword)) {
            return 80 + Math.min(level, 5);
        }
        if (aliasContains(name, keyword)) {
            return 90;
        }
        return -1;
    }

    private int compareRegionScore(RegionScore left, RegionScore right) {
        if (right == null) {
            return 1;
        }
        int scoreCompare = Integer.compare(left.score(), right.score());
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        int levelCompare = Integer.compare(level(left.region()), level(right.region()));
        if (levelCompare != 0) {
            return levelCompare;
        }
        return right.pathKey().compareTo(left.pathKey());
    }

    private int level(ErpAddressRegionVO region) {
        return region.getLevel() == null ? 0 : region.getLevel();
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
        REGION_ALIAS_SUFFIXES.forEach(suffix -> addAlias(aliases, normalized, suffix));
        return aliases;
    }

    private boolean aliasEquals(String normalizedName, String keyword) {
        for (String suffix : REGION_ALIAS_SUFFIXES) {
            if (normalizedName.endsWith(suffix) && normalizedName.length() > suffix.length()
                    && normalizedName.substring(0, normalizedName.length() - suffix.length()).equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean aliasContains(String normalizedName, String keyword) {
        for (String suffix : REGION_ALIAS_SUFFIXES) {
            if (normalizedName.endsWith(suffix) && normalizedName.length() > suffix.length()) {
                String alias = normalizedName.substring(0, normalizedName.length() - suffix.length());
                if (alias.contains(keyword) || keyword.contains(alias)) {
                    return true;
                }
            }
        }
        return false;
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
        String lowerValue = value.toLowerCase(Locale.ROOT).trim();
        if (lowerValue.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(lowerValue.length());
        for (int i = 0; i < lowerValue.length(); i++) {
            char ch = lowerValue.charAt(i);
            if (!isSearchSeparator(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private boolean isSearchSeparator(char ch) {
        return Character.isWhitespace(ch)
                || ch == '/' || ch == '／'
                || ch == ',' || ch == '，'
                || ch == ';' || ch == '；'
                || ch == ':' || ch == '：'
                || ch == '(' || ch == '（'
                || ch == ')' || ch == '）'
                || ch == '[' || ch == '【'
                || ch == ']' || ch == '】';
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
                               List<ErpAddressRegionVO> primarySearchRegions,
                               List<ErpAddressRegionVO> matchRegions) {
    }

    private record RegionScore(ErpAddressRegionVO region, int score, String pathKey) {
    }
}
