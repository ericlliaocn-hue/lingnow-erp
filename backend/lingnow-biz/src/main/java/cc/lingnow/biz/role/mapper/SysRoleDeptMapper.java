package cc.lingnow.biz.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.role.entity.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色与部门关联表 数据层
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {
}
