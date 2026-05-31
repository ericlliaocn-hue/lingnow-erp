package cc.lingnow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备类型
 *
 * @author LingNow Team
 */
@Getter
@AllArgsConstructor
public enum DeviceType {

    /**
     * PC端
     */
    PC("pc"),

    /**
     * APP端
     */
    APP("app");

    private final String device;
}
