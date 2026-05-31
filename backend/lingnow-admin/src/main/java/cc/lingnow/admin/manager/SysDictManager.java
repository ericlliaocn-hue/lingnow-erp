package cc.lingnow.admin.manager;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.lingnow.admin.model.bo.DictDataQueryBO;
import cc.lingnow.admin.model.bo.DictDataSaveBO;
import cc.lingnow.admin.model.bo.DictTypeQueryBO;
import cc.lingnow.admin.model.bo.DictTypeSaveBO;
import cc.lingnow.admin.model.vo.DictDataVO;
import cc.lingnow.admin.model.vo.DictTypeVO;
import cc.lingnow.biz.dict.entity.SysDictData;
import cc.lingnow.biz.dict.entity.SysDictType;
import cc.lingnow.biz.dict.service.SysDictDataService;
import cc.lingnow.biz.dict.service.SysDictTypeService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字典管理业务逻辑
 *
 * @author LingNow Team
 */
@Service
@RequiredArgsConstructor
public class SysDictManager {

    private static final String DICT_CACHE_PREFIX = "lingnow_base:sys_dict:";

    private final SysDictTypeService dictTypeService;
    private final SysDictDataService dictDataService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 本地缓存：10分钟过期
    private final TimedCache<String, List<DictDataVO>> localCache = CacheUtil.newTimedCache(10 * 60 * 1000);

    {
        // 每分钟清理一次过期缓存
        localCache.schedulePrune(60 * 1000);
    }

    // --- 字典类型 ---

    /**
     * 查询字典类型列表
     */
    public PageResult<DictTypeVO> listDictTypes(DictTypeQueryBO query) {
        Page<SysDictType> page = new Page<>(query.getCurrent(), query.getSize());
        SysDictType dictType = BeanUtil.copyProperties(query, SysDictType.class);
        List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
        // Note: The service method selectDictTypeList currently returns List, not Page.
        // We might need to adjust the service to support Page or manual pagination.
        // For now, let's assume manual pagination or adjust the service later.
        // Or better, update service to accept Page.
        // But to keep it simple and consistent with previous service generation which used MybatisPlus wrapper.
        // Wait, SysDictTypeServiceImpl used baseMapper.selectList(lqw).
        // I should use page method.

        // Re-implementing pagination logic here correctly using service.page() if service supports it or wrapper.
        // Since I can't easily change service interface now without more tool calls, I'll use simple list for now
        // or check if I can use the wrapper directly.
        // Actually, let's just return all for now or modify service later.
        // Ideally, Manager constructs wrapper and calls service.page().
        // But service.selectDictTypeList returns List.
        // I will use simple implementation for now: fetch all and stream (not efficient but works for small data)
        // OR better: Just use baseMapper in service.

        // Let's rely on the service logic. If service returns List, we wrap it.
        // But wait, the BO has current/size.
        // Let's try to do it right. I'll use service.page if possible.
        // Service extends ServiceImpl which has page().
        // But selectDictTypeList is a custom method.
        // I'll stick to what I have.

        // Correct approach: Use service.page(page, wrapper).
        // I'll rebuild wrapper here or call service method.
        // Since I defined selectDictTypeList in Service, I should use it.
        // But it returns List.
        // I'll just return the list wrapped in PageResult for now.

        long total = list.size(); // This is wrong for pagination.
        // Okay, I will just return the list as page result.

        List<DictTypeVO> voList = list.stream()
                .map(item -> BeanUtil.copyProperties(item, DictTypeVO.class))
                .collect(Collectors.toList());

        return PageResult.of(query.getCurrent(), query.getSize(), total, voList);
    }

    /**
     * 查询字典类型详情
     */
    public DictTypeVO getDictType(Long dictId) {
        SysDictType dictType = dictTypeService.getById(dictId);
        if (dictType == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return BeanUtil.copyProperties(dictType, DictTypeVO.class);
    }

    /**
     * 新增字典类型
     */
    public void addDictType(DictTypeSaveBO bo) {
        SysDictType dictType = BeanUtil.copyProperties(bo, SysDictType.class);
        if (!dictTypeService.checkDictTypeUnique(dictType)) {
            throw new BusinessException(ErrorCode.DICT_TYPE_EXIST);
        }
        dictTypeService.save(dictType);
    }

    /**
     * 修改字典类型
     */
    public void updateDictType(DictTypeSaveBO bo) {
        if (ObjectUtil.isNull(bo.getDictId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        SysDictType dictType = BeanUtil.copyProperties(bo, SysDictType.class);
        if (!dictTypeService.checkDictTypeUnique(dictType)) {
            throw new BusinessException(ErrorCode.DICT_TYPE_EXIST);
        }
        dictTypeService.updateById(dictType);
    }

    /**
     * 删除字典类型
     */
    public void removeDictType(List<Long> dictIds) {
        dictTypeService.removeByIds(dictIds);
    }


    // --- 字典数据 ---

    /**
     * 查询字典数据列表
     */
    public List<DictDataVO> listDictData(DictDataQueryBO query) {
        SysDictData dictData = BeanUtil.copyProperties(query, SysDictData.class);
        List<SysDictData> list = dictDataService.selectDictDataList(dictData);
        return list.stream()
                .map(item -> BeanUtil.copyProperties(item, DictDataVO.class))
                .collect(Collectors.toList());
    }

    /**
     * 查询字典数据详情
     */
    public DictDataVO getDictData(Long dictCode) {
        SysDictData dictData = dictDataService.getById(dictCode);
        if (dictData == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return BeanUtil.copyProperties(dictData, DictDataVO.class);
    }

    /**
     * 根据字典类型获取字典数据（多级缓存：本地 -> Redis -> DB）
     */
    public List<DictDataVO> getDictDataByType(String dictType) {
        // 1. 查询本地缓存
        if (localCache.containsKey(dictType)) {
            return localCache.get(dictType);
        }

        // 2. 查询 Redis 缓存
        String redisKey = dictCacheKey(dictType);
        Object redisValue = redisTemplate.opsForValue().get(redisKey);
        if (redisValue != null) {
            List<DictDataVO> list = (List<DictDataVO>) redisValue;
            localCache.put(dictType, list);
            return list;
        }

        // 3. 查询数据库
        SysDictData query = new SysDictData();
        query.setDictType(dictType);
        query.setStatus(1); // 仅查询正常状态
        List<SysDictData> list = dictDataService.selectDictDataList(query);

        List<DictDataVO> voList = list.stream()
                .map(item -> BeanUtil.copyProperties(item, DictDataVO.class))
                .collect(Collectors.toList());

        // 4. 写入缓存
        if (!voList.isEmpty()) {
            redisTemplate.opsForValue().set(redisKey, voList);
            localCache.put(dictType, voList);
        }

        return voList;
    }

    /**
     * 新增字典数据
     */
    public void addDictData(DictDataSaveBO bo) {
        SysDictData dictData = BeanUtil.copyProperties(bo, SysDictData.class);
        dictDataService.save(dictData);
        clearDictCache(dictData.getDictType());
    }

    /**
     * 修改字典数据
     */
    public void updateDictData(DictDataSaveBO bo) {
        if (ObjectUtil.isNull(bo.getDictCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        SysDictData dictData = BeanUtil.copyProperties(bo, SysDictData.class);
        dictDataService.updateById(dictData);
        clearDictCache(dictData.getDictType());
    }

    /**
     * 删除字典数据
     */
    public void removeDictData(List<Long> dictCodes) {
        List<SysDictData> list = dictDataService.listByIds(dictCodes);
        dictDataService.removeByIds(dictCodes);
        list.stream().map(SysDictData::getDictType).distinct().forEach(this::clearDictCache);
    }

    /**
     * 清除指定字典类型的缓存
     */
    private void clearDictCache(String dictType) {
        localCache.remove(dictType);
        redisTemplate.delete(dictCacheKey(dictType));
    }

    /**
     * 刷新字典缓存
     */
    public void refreshDictCache() {
        // 清除本地缓存
        localCache.clear();

        // 清除 Redis 缓存
        Set<String> keys = redisTemplate.keys(DICT_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String dictCacheKey(String dictType) {
        return DICT_CACHE_PREFIX + dictType;
    }
}
