package cc.lingnow.biz.menu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.menu.entity.SysMenu;
import cc.lingnow.biz.menu.mapper.SysMenuMapper;
import cc.lingnow.biz.menu.service.SysMenuService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统菜单服务实现
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> getMenuTree() {
        // 查询所有启用的菜单
        List<SysMenu> allMenus = baseMapper.selectAllEnabled();

        // 构建树形结构
        return buildMenuTree(allMenus, 0L);
    }

    @Override
    public List<SysMenu> getAllMenuTree() {
        // 查询所有启用的菜单
        List<SysMenu> allMenus = baseMapper.selectList(null);

        // 构建树形结构
        return buildMenuTree(allMenus, 0L);
    }

    @Override
    public List<SysMenu> getMenuTreeByUserId(Long userId) {
        // 查询用户关联的菜单
        List<SysMenu> userMenus = baseMapper.selectByUserId(userId);

        // 构建树形结构
        return buildMenuTree(userMenus, 0L);
    }

    @Override
    public List<SysMenu> getAllMenus() {
        return baseMapper.selectAllEnabled();
    }

    @Override
    public SysMenu getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public boolean save(SysMenu menu) {
        return baseMapper.insert(menu) > 0;
    }

    @Override
    public boolean update(SysMenu menu) {
        return baseMapper.updateById(menu) > 0;
    }

    @Override
    public boolean delete(Long id) {
        // 检查是否有子菜单
        List<SysMenu> children = baseMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("存在子菜单，无法删除");
        }
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(menu -> java.util.Objects.equals(menu.getParentId(), parentId))
                .peek(menu -> {
                    List<SysMenu> children = buildMenuTree(allMenus, menu.getMenuId());
                    menu.setChildren(children.isEmpty() ? null : children);
                })
                .collect(Collectors.toList());
    }
}
