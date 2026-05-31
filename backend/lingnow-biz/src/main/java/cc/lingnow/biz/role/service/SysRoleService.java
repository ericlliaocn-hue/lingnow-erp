package cc.lingnow.biz.role.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.role.entity.SysRole;
import cc.lingnow.biz.role.entity.SysUserRole;
import cc.lingnow.biz.user.entity.SysUser;

import java.util.List;
import java.util.Set;

/**
 * 角色服务类
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据用户ID查询角色键列表
     */
    Set<String> selectRoleKeysByUserId(Long userId);

    /**
     * 根据用户ID查询角色ID列表
     */
    List<Long> selectRoleIdsByUserId(Long userId);

    /**
     * 根据用户ID查询权限列表
     */
    Set<String> selectPermissionsByUserId(Long userId);

    /**
     * 为用户分配角色
     */
    void assignRoles(Long userId, Long[] roleIds);

    /**
     * 查询已分配用户角色列表
     */
    Page<SysUser> selectAllocatedList(Page<SysUser> page, SysUser user, Long roleId);

    /**
     * 查询未分配用户角色列表
     */
    Page<SysUser> selectUnallocatedList(Page<SysUser> page, SysUser user, Long roleId);

    /**
     * 取消授权用户角色
     */
    int deleteAuthUser(SysUserRole userRole);

    /**
     * 批量取消授权用户角色
     */
    int deleteAuthUsers(Long roleId, Long[] userIds);

    /**
     * 批量授权用户角色
     */
    int insertAuthUsers(Long roleId, Long[] userIds);

    /**
     * 新增角色
     */
    boolean insertRole(SysRole role);

    /**
     * 修改角色
     */
    boolean updateRole(SysRole role);

    /**
     * 修改数据权限信息
     */
    boolean authDataScope(SysRole role);

    /**
     * 根据ID查询角色
     */
    SysRole selectRoleById(Long roleId);

    /**
     * 查询所有正常状态的角色
     */
    List<SysRole> listActiveRoles();
}
