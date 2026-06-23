package cc.lingnow.admin.manager;

import cc.lingnow.admin.model.bo.SysRoleBO;
import cc.lingnow.biz.role.entity.SysRole;
import cc.lingnow.biz.role.entity.SysUserRole;
import cc.lingnow.biz.role.service.SysRoleService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 角色管理业务层
 */
@Component
@RequiredArgsConstructor
public class AdminSysRoleManager {

    private final SysRoleService roleService;
    private final SysUserService userService;

    /**
     * 分页查询角色列表
     */
    public Page<SysRole> listRoles(Page<SysRole> page, SysRoleBO query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                .like(StrUtil.isNotBlank(query.getRoleKey()), SysRole::getRoleKey, query.getRoleKey())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getSortOrder);
        return roleService.page(page, wrapper);
    }

    /**
     * 获取角色详情
     */
    public SysRole getRole(Long roleId) {
        return roleService.selectRoleById(roleId);
    }

    /**
     * 新增角色
     */
    public boolean addRole(SysRoleBO roleBO) {
        SysRole role = BeanUtil.copyProperties(roleBO, SysRole.class);
        return roleService.insertRole(role);
    }

    /**
     * 修改角色
     */
    public boolean updateRole(SysRoleBO roleBO) {
        SysRole role = BeanUtil.copyProperties(roleBO, SysRole.class);
        return roleService.updateRole(role);
    }

    /**
     * 修改数据权限
     */
    public boolean updateDataScope(SysRoleBO roleBO) {
        SysRole role = BeanUtil.copyProperties(roleBO, SysRole.class);
        return roleService.authDataScope(role);
    }

    /**
     * 删除角色
     */
    public boolean deleteRole(Long roleId) {
        return roleService.removeById(roleId);
    }

    /**
     * 获取所有激活角色
     */
    public List<SysRole> listActiveRoles() {
        return roleService.list();
    }

    /**
     * 分配角色给用户
     */
    public void assignRoles(Long userId, Long[] roleIds) {
        ensureBusinessVisibleUser(userId);
        roleService.assignRoles(userId, roleIds);
    }

    /**
     * 查询已分配用户角色列表
     */
    public Page<SysUser> selectAllocatedList(Page<SysUser> page, SysUser user, Long roleId) {
        return roleService.selectAllocatedList(page, user, roleId);
    }

    /**
     * 查询未分配用户角色列表
     */
    public Page<SysUser> selectUnallocatedList(Page<SysUser> page, SysUser user, Long roleId) {
        return roleService.selectUnallocatedList(page, user, roleId);
    }

    /**
     * 取消授权用户
     */
    public void deleteAuthUser(SysUserRole userRole) {
        ensureBusinessVisibleUser(userRole.getUserId());
        roleService.deleteAuthUser(userRole);
    }

    /**
     * 批量取消授权用户
     */
    public void deleteAuthUsers(Long roleId, String userIds) {
        Long[] ids = Arrays.stream(userIds.split(",")).map(Long::valueOf).toArray(Long[]::new);
        ensureBusinessVisibleUsers(ids);
        roleService.deleteAuthUsers(roleId, ids);
    }

    /**
     * 批量选择用户授权
     */
    public void insertAuthUsers(Long roleId, String userIds) {
        Long[] ids = Arrays.stream(userIds.split(",")).map(Long::valueOf).toArray(Long[]::new);
        ensureBusinessVisibleUsers(ids);
        roleService.insertAuthUsers(roleId, ids);
    }

    private void ensureBusinessVisibleUser(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null || userService.isInternalAccount(user)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
    }

    private void ensureBusinessVisibleUsers(Long[] userIds) {
        for (Long userId : userIds) {
            ensureBusinessVisibleUser(userId);
        }
    }
}
