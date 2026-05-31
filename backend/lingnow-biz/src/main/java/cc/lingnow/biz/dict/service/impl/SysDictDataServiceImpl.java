package cc.lingnow.biz.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.dict.entity.SysDictData;
import cc.lingnow.biz.dict.mapper.SysDictDataMapper;
import cc.lingnow.biz.dict.service.SysDictDataService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典数据Service业务层处理
 *
 * @author LingNow Team
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData) {
        LambdaQueryWrapper<SysDictData> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ObjectUtils.isNotEmpty(dictData.getDictType()), SysDictData::getDictType, dictData.getDictType())
                .like(ObjectUtils.isNotEmpty(dictData.getDictLabel()), SysDictData::getDictLabel, dictData.getDictLabel())
                .eq(dictData.getStatus() != null, SysDictData::getStatus, dictData.getStatus())
                .eq(SysDictData::getDelFlag, false)
                .orderByAsc(SysDictData::getDictSort);
        return baseMapper.selectList(lqw);
    }
}
