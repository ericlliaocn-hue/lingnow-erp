package cc.lingnow.biz.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统菜单Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询所有启用的菜单
     */
    List<SysMenu> selectAllEnabled();

    /**
     * 根据父ID查询子菜单
     */
    List<SysMenu> selectByParentId(Long parentId);

    /**
     * 根据用户ID查询菜单
     */
    List<SysMenu> selectByUserId(Long userId);
}
