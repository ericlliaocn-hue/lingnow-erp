package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "文件信息VO")
public class FileVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件访问URL")
    private String fileUrl;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "文件后缀")
    private String fileSuffix;

    @Schema(description = "存储类型")
    private String storageType;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
