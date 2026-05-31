package cc.lingnow.admin.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.notification.entity.SysUserNotification;
import cc.lingnow.biz.notification.service.SysUserNotificationService;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知控制器
 *
 * @author LingNow Team
 */
@Tag(name = "系统通知管理")
@RestController
@RequestMapping("/system/notification")
@RequiredArgsConstructor
public class SysNotificationController {

    private final SysUserNotificationService notificationService;

    @Operation(summary = "获取通知列表")
    @GetMapping("/list")
    public Result<PageResult<SysUserNotification>> list(@RequestParam(defaultValue = "1") Integer current,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) Integer isRead) {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        Page<SysUserNotification> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUserNotification> wrapper = new LambdaQueryWrapper<SysUserNotification>()
                .eq(SysUserNotification::getUserId, userId)
                .eq(isRead != null, SysUserNotification::getIsRead, isRead)
                .orderByDesc(SysUserNotification::getCreateTime);

        IPage<SysUserNotification> result = notificationService.page(page, wrapper);
        return Result.success(PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords()));
    }

    @Operation(summary = "获取未读数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @Operation(summary = "标记为已读")
    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        notificationService.readNotification(userId, id);
        return Result.success();
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        notificationService.readAllNotifications(userId);
        return Result.success();
    }
}
