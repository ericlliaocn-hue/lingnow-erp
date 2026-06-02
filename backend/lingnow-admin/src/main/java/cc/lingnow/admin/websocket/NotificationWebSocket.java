package cc.lingnow.admin.websocket;

import com.alibaba.fastjson2.JSON;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.notification.event.NotificationEvent;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 通知 WebSocket 服务
 *
 * @author LingNow Team
 */
@Slf4j
@Component
@ServerEndpoint("/ws/notification/{token}")
public class NotificationWebSocket {

    /**
     * 存放每个客户端对应的 Session 对象
     * Key: userId
     * Value: Session 集合 (支持多端登录)
     */
    private static final Map<Long, CopyOnWriteArraySet<Session>> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 发送通知
     *
     * @param event 通知事件
     */
    public static void sendNotification(NotificationEvent event) {
        CopyOnWriteArraySet<Session> sessions = SESSION_MAP.get(event.getUserId());
        if (sessions != null && !sessions.isEmpty()) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(JSON.toJSONString(event));
                        log.info("发送通知成功: userId={}, sessionId={}", event.getUserId(), session.getId());
                    } catch (IOException e) {
                        log.error("发送通知失败: userId={}, sessionId={}", event.getUserId(), session.getId(), e);
                    }
                }
            }
        } else {
            log.debug("用户不在线，无需推送: userId={}", event.getUserId());
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        try {
            // 验证 Token (使用 Admin 端的 StpLogic)
            Object loginId = StpAdminUtil.stpLogic.getLoginIdByToken(token);
            if (loginId == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Invalid Token"));
                return;
            }

            Long userId = Long.parseLong(loginId.toString());

            // 将 userId 存入 Session 属性，避免使用实例变量 (解决单例/多例潜在问题)
            session.getUserProperties().put("userId", userId);

            // 加入集合
            SESSION_MAP.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);

            log.info("WebSocket连接建立: userId={}, sessionId={}", userId, session.getId());
        } catch (Exception e) {
            log.error("WebSocket连接异常", e);
            try {
                session.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null) {
            CopyOnWriteArraySet<Session> sessions = SESSION_MAP.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    SESSION_MAP.remove(userId);
                }
            }
            log.info("WebSocket连接断开: userId={}, sessionId={}", userId, session.getId());
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        Long userId = session == null ? null : (Long) session.getUserProperties().get("userId");
        String sessionId = session == null ? null : session.getId();
        if (isNormalDisconnect(error)) {
            log.debug("WebSocket客户端连接已断开: userId={}, sessionId={}, reason={}", userId, sessionId, error.getClass().getSimpleName());
            return;
        }
        log.error("WebSocket发生错误: userId={}, sessionId={}", userId, sessionId, error);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        // 客户端发送的消息处理 (如果有)
        log.info("收到客户端消息: userId={}, message={}", userId, message);
    }

    private boolean isNormalDisconnect(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof EOFException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.contains("Broken pipe") || message.contains("Connection reset"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
