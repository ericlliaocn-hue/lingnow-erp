package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色业务对象
 */
@Data
@Schema(description = "角色业务对象")
public class SysRoleBO {

    /**
     * 角色ID
     */
    @Schema(description = "角色ID")
    private Long roleId;

    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /**
     * 角色权限字符串
     */
    @Schema(description = "角色权限字符串")
    @NotBlank(message = "权限字符不能为空")
    private String roleKey;

    /**
     * 显示顺序
     */
    @Schema(description = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    private Integer sortOrder;

    /**
     * 角色状态 (1正常 0停用)
     */
    @Schema(description = "角色状态 (1正常 0停用)")
    private Integer status;

    /**
     * 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）
     */
    @Schema(description = "数据范围")
    private Integer dataScope;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 菜单组
     */
    @Schema(description = "菜单组")
    private List<Long> menuIds;

    /**
     * 部门组（数据权限）
     */
    @Schema(description = "部门组（数据权限）")
    private List<Long> deptIds;
}
