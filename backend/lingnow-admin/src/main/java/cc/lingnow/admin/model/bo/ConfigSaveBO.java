package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 参数配置保存参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "参数配置保存参数")
public class ConfigSaveBO {

    @Schema(description = "参数ID (更新时必填)")
    private Long configId;

    @Schema(description = "参数名称")
    @NotBlank(message = "参数名称不能为空")
    private String configName;

    @Schema(description = "参数键名")
    @NotBlank(message = "参数键名不能为空")
    private String configKey;

    @Schema(description = "参数键值")
    @NotBlank(message = "参数键值不能为空")
    private String configValue;

    @Schema(description = "系统内置（Y是 N否）")
    private String configType;

    @Schema(description = "备注")
    private String remark;
}
