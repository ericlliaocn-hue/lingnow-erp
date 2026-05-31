package cc.lingnow.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 慢SQL日志事件
 *
 * @author LingNow Team
 */
@Getter
public class SlowSqlLogEvent extends ApplicationEvent {

    private final String sqlStatement;
    private final Long executionTime;
    private final Long userId;
    private final String userName;

    public SlowSqlLogEvent(Object source, String sqlStatement, Long executionTime, Long userId, String userName) {
        super(source);
        this.sqlStatement = sqlStatement;
        this.executionTime = executionTime;
        this.userId = userId;
        this.userName = userName;
    }
}
