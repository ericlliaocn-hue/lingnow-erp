package cc.lingnow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型
 *
 * @author LingNow Team
 */
@Getter
@AllArgsConstructor
public enum UserType {

    /**
     * 用户端
     */
    APP("app"),

    /**
     * 管理端
     */
    ADMIN("admin");

    private final String userType;

    public static UserType getUserType(String userType) {
        for (UserType value : UserType.values()) {
            if (value.getUserType().equals(userType)) {
                return value;
            }
        }
        return APP;
    }
}
