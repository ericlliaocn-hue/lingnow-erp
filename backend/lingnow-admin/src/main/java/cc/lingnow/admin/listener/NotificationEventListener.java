package cc.lingnow.admin.listener;

import cc.lingnow.admin.websocket.NotificationWebSocket;
import cc.lingnow.biz.notification.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 通知事件监听器
 *
 * @author LingNow Team
 */
@Slf4j
@Component
public class NotificationEventListener {

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("收到通知事件: {}", event);
        NotificationWebSocket.sendNotification(event);
    }
}
