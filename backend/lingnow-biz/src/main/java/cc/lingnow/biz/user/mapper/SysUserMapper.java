package cc.lingnow.biz.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.lingnow.biz.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper
 *
 * @author LingNow Team
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据条件分页查询已分配用户角色列表
     */
    Page<SysUser> selectAllocatedList(@Param("page") Page<SysUser> page, @Param("user") SysUser user, @Param("roleId") Long roleId);

    /**
     * 根据条件分页查询未分配用户角色列表
     */
    Page<SysUser> selectUnallocatedList(@Param("page") Page<SysUser> page, @Param("user") SysUser user, @Param("roleId") Long roleId);
}
