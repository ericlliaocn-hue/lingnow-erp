package cc.lingnow.biz.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cc.lingnow.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件存储配置实体
 *
 * @author LingNow Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file_config")
@Schema(description = "文件存储配置实体")
public class SysFileConfig extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 平台: LOCAL, MINIO, ALIYUN, TENCENT, QINIU, REST
     */
    @Schema(description = "平台类型")
    private String platform;

    /**
     * 配置信息(JSON)
     */
    @Schema(description = "配置信息(JSON)")
    private String configJson;

    /**
     * 是否启用: 0-否, 1-是
     */
    @Schema(description = "是否启用")
    private Integer isActive;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
}
