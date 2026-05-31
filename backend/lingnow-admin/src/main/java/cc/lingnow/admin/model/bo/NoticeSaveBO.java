package cc.lingnow.admin.model.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通知公告保存参数
 *
 * @author LingNow Team
 */
@Data
@Schema(description = "通知公告保存参数")
public class NoticeSaveBO {

    @Schema(description = "公告ID (更新时必填)")
    private Long noticeId;

    @Schema(description = "公告标题")
    @NotBlank(message = "公告标题不能为空")
    private String noticeTitle;

    @Schema(description = "公告类型（1通知 2公告）")
    @NotBlank(message = "公告类型不能为空")
    private String noticeType;

    @Schema(description = "公告内容")
    private String noticeContent;

    @Schema(description = "公告状态（1正常 0关闭）")
    private Boolean status;

    @Schema(description = "备注")
    private String remark;
}
