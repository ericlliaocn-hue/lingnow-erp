package cc.lingnow.biz.menu.service;

import cc.lingnow.biz.menu.entity.SysMenu;

import java.util.List;

/**
 * 系统菜单服务接口
 */
public interface SysMenuService {

    /**
     * 获取菜单树
     */
    List<SysMenu> getMenuTree();

    List<SysMenu> getAllMenuTree();

    /**
     * 根据用户ID获取菜单树
     */
    List<SysMenu> getMenuTreeByUserId(Long userId);

    /**
     * 获取所有菜单列表
     */
    List<SysMenu> getAllMenus();

    /**
     * 根据ID获取菜单
     */
    SysMenu getById(Long id);

    /**
     * 新增菜单
     */
    boolean save(SysMenu menu);

    /**
     * 更新菜单
     */
    boolean update(SysMenu menu);

    /**
     * 删除菜单
     */
    boolean delete(Long id);
}
