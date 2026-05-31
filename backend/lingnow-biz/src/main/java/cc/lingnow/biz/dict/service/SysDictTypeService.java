package cc.lingnow.biz.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.dict.entity.SysDictType;

import java.util.List;

/**
 * 字典类型Service接口
 *
 * @author LingNow Team
 */
public interface SysDictTypeService extends IService<SysDictType> {

    /**
     * 根据条件分页查询字典类型
     *
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    List<SysDictType> selectDictTypeList(SysDictType dictType);

    /**
     * 校验字典类型称是否唯一
     *
     * @param dict 字典类型
     * @return 结果
     */
    boolean checkDictTypeUnique(SysDictType dict);
}
