package cc.lingnow.common.enums;

/**
 * 限流类型
 *
 * @author LingNow Team
 */
public enum LimitType {
    /**
     * 默认策略全局限流
     */
    DEFAULT,

    /**
     * 根据请求者IP进行限流
     */
    IP,

    /**
     * 根据请求者进行限流 (登录用户ID)
     */
    USER
}
