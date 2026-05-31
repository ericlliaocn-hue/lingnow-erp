package cc.lingnow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 连载状态枚举
 *
 * @author LingNow Team
 */
@Getter
@AllArgsConstructor
public enum SerialStatus {

    /**
     * 连载中
     */
    SERIALIZING("SERIALIZING", "连载中"),

    /**
     * 已完结
     */
    COMPLETED("COMPLETED", "已完结");

    @EnumValue
    @JsonValue
    private final String code;

    private final String info;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static SerialStatus of(String value) {
        if (value == null) {
            return null;
        }
        for (SerialStatus item : values()) {
            if (item.getCode().equals(value)) {
                return item;
            }
        }
        return null;
    }
}
