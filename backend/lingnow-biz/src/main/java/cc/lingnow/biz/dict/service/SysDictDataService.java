package cc.lingnow.biz.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.dict.entity.SysDictData;

import java.util.List;

/**
 * 字典数据Service接口
 *
 * @author LingNow Team
 */
public interface SysDictDataService extends IService<SysDictData> {

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    List<SysDictData> selectDictDataList(SysDictData dictData);
}
