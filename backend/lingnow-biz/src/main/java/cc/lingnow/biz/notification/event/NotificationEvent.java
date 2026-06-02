package cc.lingnow.biz.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通知事件
 *
 * @author LingNow Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private Long userId;
    private String title;
    private String content;
    private String type;
    private Long bizId;
    private String bizType;
    private String category;
    private String actionType;
    private String actionUrl;
}
