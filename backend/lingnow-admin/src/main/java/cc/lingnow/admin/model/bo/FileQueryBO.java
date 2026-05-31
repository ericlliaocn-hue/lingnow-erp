package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件查询参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "文件查询参数")
public class FileQueryBO {

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "存储类型")
    private String storageType;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
