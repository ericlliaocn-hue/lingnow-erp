package cc.lingnow.biz.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.notification.entity.SysUserNotification;
import cc.lingnow.biz.notification.event.NotificationEvent;
import cc.lingnow.biz.notification.mapper.SysUserNotificationMapper;
import cc.lingnow.biz.notification.service.SysUserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户系统通知服务实现
 *
 * @author LingNow Team
 */
@Service
@RequiredArgsConstructor
public class SysUserNotificationServiceImpl extends ServiceImpl<SysUserNotificationMapper, SysUserNotification> implements SysUserNotificationService {

    private static final String UNREAD_COUNT_KEY = "sys:notify:unread:";
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void sendNotification(Long userId, String title, String content, String type, Long bizId, String bizType) {
        SysUserNotification notification = new SysUserNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type == null ? "info" : type);
        notification.setIsRead(0);
        notification.setBizId(bizId);
        notification.setBizType(bizType);
        this.save(notification);

        // 更新缓存
        String key = UNREAD_COUNT_KEY + userId;
        redisTemplate.opsForValue().increment(key);

        // 发布事件
        eventPublisher.publishEvent(new NotificationEvent(userId, title, content, notification.getType(), bizId, bizType));
    }

    @Override
    public long getUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY + userId;
        String val = redisTemplate.opsForValue().get(key);
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        long count = this.count(new LambdaQueryWrapper<SysUserNotification>()
                .eq(SysUserNotification::getUserId, userId)
                .eq(SysUserNotification::getIsRead, 0));

        redisTemplate.opsForValue().set(key, String.valueOf(count));
        return count;
    }

    @Override
    public void readNotification(Long userId, Long id) {
        SysUserNotification notification = this.getById(id);
        if (notification != null && notification.getUserId().equals(userId) && notification.getIsRead() == 0) {
            notification.setIsRead(1);
            this.updateById(notification);

            String key = UNREAD_COUNT_KEY + userId;
            // 保证不为负数
            Long val = redisTemplate.opsForValue().decrement(key);
            if (val != null && val < 0) {
                redisTemplate.opsForValue().set(key, "0");
            }
        }
    }

    @Override
    public void readAllNotifications(Long userId) {
        this.update(new LambdaUpdateWrapper<SysUserNotification>()
                .eq(SysUserNotification::getUserId, userId)
                .eq(SysUserNotification::getIsRead, 0)
                .set(SysUserNotification::getIsRead, 1));

        String key = UNREAD_COUNT_KEY + userId;
        redisTemplate.delete(key);
    }
}
