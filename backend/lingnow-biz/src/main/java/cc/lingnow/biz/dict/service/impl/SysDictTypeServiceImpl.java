package cc.lingnow.biz.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.dict.entity.SysDictType;
import cc.lingnow.biz.dict.mapper.SysDictTypeMapper;
import cc.lingnow.biz.dict.service.SysDictTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典类型Service业务层处理
 *
 * @author LingNow Team
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType) {
        LambdaQueryWrapper<SysDictType> lqw = new LambdaQueryWrapper<>();
        lqw.like(ObjectUtils.isNotEmpty(dictType.getDictName()), SysDictType::getDictName, dictType.getDictName())
                .like(ObjectUtils.isNotEmpty(dictType.getDictType()), SysDictType::getDictType, dictType.getDictType())
                .eq(dictType.getStatus() != null, SysDictType::getStatus, dictType.getStatus())
                .eq(SysDictType::getDelFlag, false)
                .orderByDesc(SysDictType::getCreateTime);
        return baseMapper.selectList(lqw);
    }

    @Override
    public boolean checkDictTypeUnique(SysDictType dict) {
        Long dictId = ObjectUtils.isEmpty(dict.getDictId()) ? -1L : dict.getDictId();
        SysDictType dictType = baseMapper.selectOne(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dict.getDictType())
                .last("limit 1"));
        return ObjectUtils.isEmpty(dictType) || dictType.getDictId().longValue() == dictId.longValue();
    }
}
