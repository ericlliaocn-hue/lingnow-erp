package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "菜单视图对象")
public class SysMenuVO {

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型：0目录 1菜单 2按钮")
    private Integer menuType;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "显示顺序")
    private Integer sortOrder;

    @Schema(description = "是否可见：0隐藏 1显示")
    private Integer visible;

    @Schema(description = "是否缓存：Y缓存 N不缓存")
    private String isCache;

    @Schema(description = "状态：0禁用 1启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "子菜单")
    private List<SysMenuVO> children;
}
