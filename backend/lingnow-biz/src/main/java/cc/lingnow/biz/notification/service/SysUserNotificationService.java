package cc.lingnow.biz.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.notification.entity.SysUserNotification;

/**
 * 用户系统通知服务接口
 *
 * @author LingNow Team
 */
public interface SysUserNotificationService extends IService<SysUserNotification> {

    /**
     * 发送通知
     *
     * @param userId  接收用户ID
     * @param title   标题
     * @param content 内容
     * @param type    类型
     * @param bizId   业务ID
     * @param bizType 业务类型
     */
    void sendNotification(Long userId, String title, String content, String type, Long bizId, String bizType);

    /**
     * 获取未读数量
     * sendNotification
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    long getUnreadCount(Long userId);

    /**
     * 标记通知为已读
     *
     * @param userId 用户ID
     * @param id     通知ID
     */
    void readNotification(Long userId, Long id);

    /**
     * 标记所有通知为已读
     *
     * @param userId 用户ID
     */
    void readAllNotifications(Long userId);
}
