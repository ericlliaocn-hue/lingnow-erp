package cc.lingnow.admin.config;

import cn.dev33.satoken.stp.StpInterface;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.admin.util.StpShopUtil;
import cc.lingnow.biz.role.service.SysRoleService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 自定义权限加载接口实现类
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysRoleService roleService;
    private final SysUserService userService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 仅处理 admin 端的权限
        if (StpAdminUtil.TYPE.equals(loginType)) {
            Long userId = Long.valueOf(loginId.toString());
            // 超级管理员账号直接返回所有权限
            SysUser user = userService.getById(userId);
            if (userService.isSuperAdmin(user)) {
                return List.of("*:*:*");
            }

            Set<String> permissions = roleService.selectPermissionsByUserId(userId);
            return new ArrayList<>(permissions);
        }
        if (StpShopUtil.TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 仅处理 admin 端的角色
        if (StpAdminUtil.TYPE.equals(loginType)) {
            Long userId = Long.valueOf(loginId.toString());
            // 超级管理员账号直接返回 admin 角色
            SysUser user = userService.getById(userId);
            if (userService.isSuperAdmin(user)) {
                return List.of("admin");
            }

            Set<String> roles = roleService.selectRoleKeysByUserId(userId);
            return new ArrayList<>(roles);
        }
        if (StpShopUtil.TYPE.equals(loginType)) {
            return List.of("shop_customer");
        }
        return new ArrayList<>();
    }

}
