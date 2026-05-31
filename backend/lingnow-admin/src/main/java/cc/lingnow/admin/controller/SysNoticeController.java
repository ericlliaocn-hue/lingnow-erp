package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.SysNoticeManager;
import cc.lingnow.admin.model.bo.NoticeQueryBO;
import cc.lingnow.admin.model.bo.NoticeSaveBO;
import cc.lingnow.admin.model.vo.NoticeVO;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知公告控制器
 *
 * @author LingNow Team
 */
@Tag(name = "通知公告管理", description = "通知公告管理接口")
@RestController
@RequestMapping("/system/notice")
@RequiredArgsConstructor
@Validated
public class SysNoticeController {

    private final SysNoticeManager noticeManager;

    @Operation(summary = "获取通知公告列表")
    @GetMapping("/list")
    public Result<PageResult<NoticeVO>> list(NoticeQueryBO query) {
        return Result.success(noticeManager.listNotices(query));
    }

    @Operation(summary = "获取通知公告详细信息")
    @GetMapping("/{noticeId}")
    public Result<NoticeVO> getInfo(@PathVariable Long noticeId) {
        return Result.success(noticeManager.getNotice(noticeId));
    }

    @Operation(summary = "新增通知公告")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody NoticeSaveBO bo) {
        noticeManager.addNotice(bo);
        return Result.success();
    }

    @Operation(summary = "修改通知公告")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody NoticeSaveBO bo) {
        noticeManager.updateNotice(bo);
        return Result.success();
    }

    @Operation(summary = "删除通知公告")
    @DeleteMapping("/{noticeIds}")
    public Result<Void> remove(@PathVariable List<Long> noticeIds) {
        noticeManager.removeNotice(noticeIds);
        return Result.success();
    }
}
