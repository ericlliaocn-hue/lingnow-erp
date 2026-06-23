package cc.lingnow.admin.manager;

import cc.lingnow.admin.model.bo.SysMenuBO;
import cc.lingnow.admin.model.vo.SysMenuVO;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.menu.entity.SysMenu;
import cc.lingnow.biz.menu.service.SysMenuService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理业务层
 */
@Component
@RequiredArgsConstructor
public class AdminSysMenuManager {

    private final SysMenuService menuService;
    private final SysUserService userService;

    /**
     * 获取当前用户菜单树
     */
    public List<SysMenuVO> getMenuTree() {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);

        List<SysMenu> menus;
        // 超级管理员账号直接返回所有菜单
        if (userService.isInternalAccount(user)) {
            menus = menuService.getAllMenuTree();
        } else {
            menus = menuService.getMenuTreeByUserId(userId);
        }

        return toVOList(menus);
    }

    /**
     * 获取所有菜单树
     */
    public List<SysMenuVO> getAllMenuTree() {
        return toVOList(menuService.getAllMenuTree());
    }

    /**
     * 获取所有菜单列表
     */
    public List<SysMenuVO> listMenus() {
        return toVOList(menuService.getAllMenus());
    }

    /**
     * 获取菜单详情
     */
    public SysMenuVO getMenu(Long id) {
        return toVO(menuService.getById(id));
    }

    /**
     * 新增菜单
     */
    public boolean addMenu(SysMenuBO menuBO) {
        SysMenu menu = BeanUtil.copyProperties(menuBO, SysMenu.class);
        return menuService.save(menu);
    }

    /**
     * 修改菜单
     */
    public boolean updateMenu(SysMenuBO menuBO) {
        SysMenu menu = BeanUtil.copyProperties(menuBO, SysMenu.class);
        return menuService.update(menu);
    }

    /**
     * 删除菜单
     */
    public boolean deleteMenu(Long id) {
        return menuService.delete(id);
    }

    private List<SysMenuVO> toVOList(List<SysMenu> list) {
        if (list == null) return null;
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    private SysMenuVO toVO(SysMenu entity) {
        if (entity == null) return null;
        SysMenuVO vo = BeanUtil.copyProperties(entity, SysMenuVO.class);
        if (entity.getChildren() != null) {
            vo.setChildren(toVOList(entity.getChildren()));
        }
        return vo;
    }
}
