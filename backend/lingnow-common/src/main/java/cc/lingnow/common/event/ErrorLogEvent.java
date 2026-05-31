package cc.lingnow.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 错误日志事件
 *
 * @author LingNow Team
 */
@Getter
public class ErrorLogEvent extends ApplicationEvent {

    private final String requestUrl;
    private final String requestMethod;
    private final String requestParams;
    private final String ip;
    private final String errorMsg;
    private final String errorStack;
    private final Long userId;
    private final String userName;

    public ErrorLogEvent(Object source, String requestUrl, String requestMethod, String requestParams,
                         String ip, String errorMsg, String errorStack, Long userId, String userName) {
        super(source);
        this.requestUrl = requestUrl;
        this.requestMethod = requestMethod;
        this.requestParams = requestParams;
        this.ip = ip;
        this.errorMsg = errorMsg;
        this.errorStack = errorStack;
        this.userId = userId;
        this.userName = userName;
    }
}
