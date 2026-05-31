package cc.lingnow.common.vo;

import cc.lingnow.common.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回结果
 *
 * @author LingNow Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一返回结果")
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    @Schema(description = "状态码")
    private Integer code;

    /**
     * 返回消息
     */
    @Schema(description = "返回消息")
    private String message;

    /**
     * 返回数据
     */
    @Schema(description = "返回数据")
    private T data;

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功返回（有数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功返回（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败返回（自定义状态码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 自定义返回
     */
    public static <T> Result<T> of(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

}
