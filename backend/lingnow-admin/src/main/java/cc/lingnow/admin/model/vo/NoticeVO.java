package cc.lingnow.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告VO
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "通知公告信息VO")
public class NoticeVO {

    @Schema(description = "公告ID")
    private Long noticeId;

    @Schema(description = "公告标题")
    private String noticeTitle;

    @Schema(description = "公告类型（1通知 2公告）")
    private String noticeType;

    @Schema(description = "公告内容")
    private String noticeContent;

    @Schema(description = "公告状态（1正常 0关闭）")
    private Boolean status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建者")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
