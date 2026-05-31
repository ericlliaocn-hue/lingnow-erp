package cc.lingnow.biz.menu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cc.lingnow.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 系统菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 菜单ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long menuId;

    /**
     * 父菜单ID，0表示根菜单
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    private String menuName;

    /**
     * 菜单类型：0目录 1菜单 2按钮
     */
    @TableField("menu_type")
    private Integer menuType;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 显示顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否可见：0隐藏 1显示
     */
    private Integer visible;

    /**
     * 状态：0禁用 1启用
     */
    private Integer status;

    /**
     * 是否缓存：Y缓存 N不缓存
     */
    @TableField("is_cache")
    private String isCache;

    /**
     * 备注
     */
    private String remark;

    /**
     * 子菜单列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysMenu> children;
}
