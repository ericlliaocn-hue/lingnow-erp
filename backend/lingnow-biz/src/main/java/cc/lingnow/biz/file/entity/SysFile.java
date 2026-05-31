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
 * 文件信息实体
 *
 * @author LingNow Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
@Schema(description = "文件信息实体")
public class SysFile extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String fileName;

    /**
     * 文件存储路径
     */
    @Schema(description = "文件存储路径")
    private String filePath;

    /**
     * 文件访问URL
     */
    @Schema(description = "文件访问URL")
    private String fileUrl;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 文件后缀
     */
    @Schema(description = "文件后缀")
    private String fileSuffix;

    /**
     * 存储类型: LOCAL, MINIO, ALIYUN, TENCENT, QINIU, REST
     */
    @Schema(description = "存储类型")
    private String storageType;

    /**
     * 业务ID
     */
    @Schema(description = "业务ID")
    private Long businessId;

    /**
     * 业务类型(表名)
     */
    @Schema(description = "业务类型")
    private String businessType;

}
