package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单业务对象
 */
@Data
@Schema(description = "菜单业务对象")
public class SysMenuBO {

    /**
     * 菜单ID
     */
    @Schema(description = "菜单ID")
    private Long menuId;

    /**
     * 父菜单ID
     */
    @Schema(description = "父菜单ID")
    private Long parentId;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称")
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    /**
     * 菜单类型：0目录 1菜单 2按钮
     */
    @Schema(description = "菜单类型：0目录 1菜单 2按钮")
    @NotNull(message = "菜单类型不能为空")
    private Integer menuType;

    /**
     * 菜单图标
     */
    @Schema(description = "菜单图标")
    private String icon;

    /**
     * 路由地址
     */
    @Schema(description = "路由地址")
    private String path;

    /**
     * 组件路径
     */
    @Schema(description = "组件路径")
    private String component;

    /**
     * 权限标识
     */
    @Schema(description = "权限标识")
    private String permission;

    /**
     * 显示顺序
     */
    @Schema(description = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    private Integer sortOrder;

    /**
     * 是否可见：0隐藏 1显示
     */
    @Schema(description = "是否可见：0隐藏 1显示")
    private Integer visible;

    /**
     * 是否缓存：Y缓存 N不缓存
     */
    @Schema(description = "是否缓存：Y缓存 N不缓存")
    private String isCache;

    /**
     * 状态：0禁用 1启用
     */
    @Schema(description = "状态：0禁用 1启用")
    private Integer status;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
}
