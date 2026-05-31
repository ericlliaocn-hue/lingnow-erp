package cc.lingnow.biz.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.menu.entity.SysMenu;
import cc.lingnow.biz.menu.mapper.SysMenuMapper;
import cc.lingnow.biz.role.entity.SysRole;
import cc.lingnow.biz.role.entity.SysRoleDept;
import cc.lingnow.biz.role.entity.SysRoleMenu;
import cc.lingnow.biz.role.entity.SysUserRole;
import cc.lingnow.biz.role.mapper.SysRoleDeptMapper;
import cc.lingnow.biz.role.mapper.SysRoleMapper;
import cc.lingnow.biz.role.mapper.SysRoleMenuMapper;
import cc.lingnow.biz.role.mapper.SysUserRoleMapper;
import cc.lingnow.biz.role.service.SysRoleService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserMapper userMapper;

    @Override
    public Set<String> selectRoleKeysByUserId(Long userId) {
        List<SysRole> roles = baseMapper.selectRolesByUserId(userId);
        return roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
    }

    @Override
    public List<Long> selectRoleIdsByUserId(Long userId) {
        List<SysUserRole> list = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        return list.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    public Set<String> selectPermissionsByUserId(Long userId) {
        // 1. 如果是超级管理员(通常约定角色健为 admin)，返回所有权限
        Set<String> roles = selectRoleKeysByUserId(userId);
        if (roles.contains("admin")) {
            return Set.of("*:*:*");
        }

        // 2. 查询该用户所有角色关联的菜单权限
        // 这里简化实现，实际可能需要更复杂的关联查询
        // 假设 SysMenu 中有 permission 字段
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .exists("SELECT 1 FROM sys_role_menu rm " +
                        "LEFT JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
                        "WHERE rm.menu_id = sys_menu.menu_id AND ur.user_id = {0}", userId)
                .isNotNull(SysMenu::getPermission)
                .ne(SysMenu::getPermission, ""));

        return menus.stream().map(SysMenu::getPermission).collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, Long[] roleIds) {
        // 先删除原有关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));

        // 再建立新关联
        if (roleIds != null && roleIds.length > 0) {
            Arrays.stream(roleIds).forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            });
        }
    }

    @Override
    public Page<SysUser> selectAllocatedList(Page<SysUser> page, SysUser user, Long roleId) {
        return userMapper.selectAllocatedList(page, user, roleId);
    }

    @Override
    public Page<SysUser> selectUnallocatedList(Page<SysUser> page, SysUser user, Long roleId) {
        return userMapper.selectUnallocatedList(page, user, roleId);
    }

    @Override
    public int deleteAuthUser(SysUserRole userRole) {
        return userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, userRole.getRoleId())
                .eq(SysUserRole::getUserId, userRole.getUserId()));
    }

    @Override
    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        return userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId)
                .in(SysUserRole::getUserId, Arrays.asList(userIds)));
    }

    @Override
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        // 新增用户与角色管理
        List<SysUserRole> list = new ArrayList<>();
        for (Long userId : userIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        if (list.size() > 0) {
            // Mybatis-Plus IService batch insert not available here easily without casting or looping
            // Loop insert is fine for small batches, or use custom batch insert
            for (SysUserRole ur : list) {
                userRoleMapper.insert(ur);
            }
            return list.size();
        }
        return 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertRole(SysRole role) {
        // 新增角色信息
        boolean result = save(role);
        // 新增角色菜单关联
        insertRoleMenu(role);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRole role) {
        // 修改角色信息
        boolean result = updateById(role);
        // 删除角色与菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getRoleId()));
        // 新增角色与菜单关联
        insertRoleMenu(role);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean authDataScope(SysRole role) {
        // 修改角色信息
        boolean result = updateById(role);
        // 删除角色与部门关联
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getRoleId()));
        // 新增角色与部门关联
        if (role.getDeptIds() != null && !role.getDeptIds().isEmpty()) {
            List<SysRoleDept> list = new ArrayList<>();
            for (Long deptId : role.getDeptIds()) {
                SysRoleDept rd = new SysRoleDept();
                rd.setRoleId(role.getRoleId());
                rd.setDeptId(deptId);
                list.add(rd);
            }
            if (!list.isEmpty()) {
                for (SysRoleDept rd : list) {
                    roleDeptMapper.insert(rd);
                }
            }
        }
        return result;
    }

    @Override
    public SysRole selectRoleById(Long roleId) {
        SysRole role = getById(roleId);
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        role.setMenuIds(roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList()));
        List<SysRoleDept> roleDepts = roleDeptMapper.selectList(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        role.setDeptIds(roleDepts.stream().map(SysRoleDept::getDeptId).collect(Collectors.toList()));
        return role;
    }

    public void insertRoleMenu(SysRole role) {
        List<Long> menuIds = role.getMenuIds();
        if (menuIds != null && menuIds.size() > 0) {
            List<SysRoleMenu> list = new ArrayList<>();
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(role.getRoleId());
                rm.setMenuId(menuId);
                list.add(rm);
            }
            for (SysRoleMenu rm : list) {
                roleMenuMapper.insert(rm);
            }
        }
    }

    @Override
    public List<SysRole> listActiveRoles() {
        return list(new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1));
    }
}
