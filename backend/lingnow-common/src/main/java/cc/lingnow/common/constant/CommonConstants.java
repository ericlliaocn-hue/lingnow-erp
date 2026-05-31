package cc.lingnow.common.constant;

/**
 * 公共常量
 *
 * @author LingNow Team
 */
public class CommonConstants {

    /**
     * 删除标记 - 未删除
     */
    public static final Integer DEL_FLAG_NORMAL = 0;

    /**
     * 删除标记 - 已删除
     */
    public static final Integer DEL_FLAG_DELETED = 1;

    /**
     * 状态 - 正常
     */
    public static final Integer STATUS_NORMAL = 1;

    /**
     * 状态 - 禁用
     */
    public static final Integer STATUS_DISABLED = 0;

    /**
     * 成功标记
     */
    public static final Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    public static final Integer FAIL = 500;

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Token 在 Header 中的 key
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Redis Key 前缀
     */
    public static final String REDIS_KEY_PREFIX = "lingnow:";

    /**
     * 用户缓存 Key 前缀
     */
    public static final String USER_CACHE_KEY = REDIS_KEY_PREFIX + "user:";

    /**
     * 验证码缓存 Key 前缀
     */
    public static final String CAPTCHA_CACHE_KEY = REDIS_KEY_PREFIX + "captcha:";

    /**
     * 匹配队列 Key
     */
    public static final String MATCH_QUEUE_KEY = REDIS_KEY_PREFIX + "match:queue";

    /**
     * UTF-8 编码
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 分页默认页码
     */
    public static final Long DEFAULT_CURRENT = 1L;

    /**
     * 分页默认每页大小
     */
    public static final Long DEFAULT_SIZE = 10L;

    /**
     * 分页最大每页大小
     */
    public static final Long MAX_SIZE = 100L;

}
