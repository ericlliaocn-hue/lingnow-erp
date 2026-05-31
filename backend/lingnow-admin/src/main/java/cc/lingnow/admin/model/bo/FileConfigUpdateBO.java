package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件配置更新参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "文件配置更新参数")
public class FileConfigUpdateBO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "平台: LOCAL, MINIO, ALIYUN, etc.")
    private String platform;

    @Schema(description = "配置信息(JSON)")
    private String configJson;

    @Schema(description = "是否启用: 0-否, 1-是")
    private Integer isActive;

    @Schema(description = "备注")
    private String remark;
}
