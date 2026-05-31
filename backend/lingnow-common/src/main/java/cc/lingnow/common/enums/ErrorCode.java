package cc.lingnow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 *
 * @author LingNow Team
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误码 1xxx
    SUCCESS(200, "操作成功"),
    NOT_LOGIN(401, "未登录"),
    PARAMS_ERROR(1001, "参数错误"),
    NO_AUTH(1002, "无权限"),
    NOT_FOUND(1003, "资源不存在"),
    BUSINESS_ERROR(1004, "业务异常"),
    SYSTEM_ERROR(1005, "系统异常"),

    // 用户相关 2xxx
    USER_NOT_EXIST(2001, "用户不存在"),
    USER_EXIST(2002, "用户已存在"),
    PASSWORD_ERROR(2003, "密码错误"),
    VERIFY_CODE_ERROR(2006, "验证码错误"),
    USER_DISABLED(2004, "用户已被禁用"),
    USER_LOCKED(2005, "用户已被锁定"),

    // 匹配相关 3xxx
    MATCH_NOT_FOUND(3001, "匹配记录不存在"),
    MATCH_IN_PROGRESS(3002, "正在匹配中"),
    MATCH_FAILED(3003, "匹配失败"),

    // 消息相关 4xxx
    MESSAGE_NOT_FOUND(4001, "消息不存在"),
    MESSAGE_SEND_FAILED(4002, "消息发送失败"),
    CONVERSATION_NOT_FOUND(4003, "会话不存在"),

    // 文件相关 5xxx
    FILE_UPLOAD_FAILED(5001, "文件上传失败"),
    FILE_TYPE_ERROR(5002, "文件类型错误"),
    FILE_SIZE_EXCEEDED(5003, "文件大小超限"),

    // 系统管理相关 6xxx
    DATA_NOT_EXIST(6001, "数据不存在"),
    PARAM_ERROR(6002, "参数错误"),

    // 部门错误
    DEPT_NAME_EXIST(6101, "部门名称已存在"),
    DEPT_PARENT_ID_ERROR(6102, "父部门不能是自己"),
    DEPT_HAS_CHILD(6103, "存在子部门,不允许删除"),
    DEPT_HAS_USER(6104, "部门下存在用户,不允许删除"),

    // 岗位错误
    POST_NAME_EXIST(6201, "岗位名称已存在"),
    POST_CODE_EXIST(6202, "岗位编码已存在"),

    // 字典错误
    DICT_TYPE_EXIST(6301, "字典类型已存在"),

    // 参数配置错误
    CONFIG_KEY_EXIST(6401, "参数键名已存在");


    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

}
