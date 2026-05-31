package cc.lingnow.biz.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cc.lingnow.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 系统角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@Schema(description = "系统角色实体")
public class SysRole extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long roleId;
    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 角色权限字符串
     */
    @Schema(description = "角色权限字符串")
    private String roleKey;

    /**
     * 显示顺序
     */
    @Schema(description = "显示顺序")
    private Integer sortOrder;

    /**
     * 角色状态 (1正常 0停用)
     */
    @Schema(description = "角色状态")
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
    @TableField(exist = false)
    private List<Long> menuIds;

    /**
     * 部门组（数据权限）
     */
    @TableField(exist = false)
    private List<Long> deptIds;
}
