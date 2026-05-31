package cc.lingnow.admin.websocket;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.alibaba.fastjson2.JSON;
import cc.lingnow.admin.util.StpAdminUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统日志 WebSocket 服务
 *
 * @author LingNow Team
 */
@Slf4j
@Component
@ServerEndpoint("/ws/log/{token}")
public class LogWebSocket {

    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private static WebSocketAppender appender;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        try {
            // 验证 Token
            Object loginId = StpAdminUtil.stpLogic.getLoginIdByToken(token);
            if (loginId == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Invalid Token"));
                return;
            }
            if (!StpAdminUtil.stpLogic.hasPermission(loginId, "monitor:log:view")) {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Permission denied"));
                return;
            }

            SESSIONS.put(session.getId(), session);
            initAppender();

            // 发送一条欢迎消息
            LogEntry welcome = new LogEntry();
            welcome.setId(System.currentTimeMillis());
            welcome.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
            welcome.setLevel("INFO");
            welcome.setThread("WebSocket");
            welcome.setLogger("System");
            welcome.setMessage("日志监控连接成功，开始接收实时日志...");
            session.getBasicRemote().sendText(JSON.toJSONString(welcome));

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
        SESSIONS.remove(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        SESSIONS.remove(session.getId());
    }

    /**
     * 初始化 Appender
     */
    private synchronized void initAppender() {
        if (appender == null) {
            try {
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                ch.qos.logback.classic.Logger rootLogger = lc.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

                appender = new WebSocketAppender();
                appender.setContext(lc);
                appender.setName("WEBSOCKET_LOG_MONITOR");
                appender.start();
                rootLogger.addAppender(appender);
                log.info("WebSocket日志监控 Appender 已启动");
            } catch (Exception e) {
                log.error("启动日志监控 Appender 失败", e);
            }
        }
    }

    /**
     * 自定义 Logback Appender
     */
    public static class WebSocketAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            if (SESSIONS.isEmpty()) {
                return;
            }

            try {
                // 过滤掉本类的日志，防止死循环
                if (event.getLoggerName().equals(LogWebSocket.class.getName())) {
                    return;
                }

                LogEntry entry = new LogEntry();
                entry.setId(System.currentTimeMillis());
                entry.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(event.getTimeStamp())));
                entry.setLevel(event.getLevel().toString());
                entry.setThread(event.getThreadName());
                entry.setLogger(event.getLoggerName());
                entry.setMessage(event.getFormattedMessage());

                String json = JSON.toJSONString(entry);

                for (Session session : SESSIONS.values()) {
                    if (session.isOpen()) {
                        try {
                            session.getBasicRemote().sendText(json);
                        } catch (IOException e) {
                            // 忽略发送错误
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略处理异常
            }
        }
    }

    @Data
    static class LogEntry {
        private long id;
        private String timestamp;
        private String level;
        private String thread;
        private String logger;
        private String message;
    }
}
