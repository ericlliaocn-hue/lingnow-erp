package cc.lingnow.admin.util;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Admin 端权限认证工具类
 * 使用独立的 StpLogic，与 App 端隔离
 *
 * @author LingNow Team
 */
public class StpAdminUtil {

    /**
     * 账号类型标识
     */
    public static final String TYPE = "admin";

    /**
     * Admin 端专用的 StpLogic
     */
    public static StpLogic stpLogic = new StpLogic(TYPE);

    /**
     * 登录
     */
    public static void login(Object id) {
        stpLogic.login(id);
    }

    /**
     * 登出
     */
    public static void logout() {
        stpLogic.logout();
    }

    /**
     * 检查是否登录
     */
    public static void checkLogin() {
        stpLogic.checkLogin();
    }

    /**
     * 是否登录
     */
    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    /**
     * 获取当前登录的管理员ID
     */
    public static long getLoginIdAsLong() {
        return stpLogic.getLoginIdAsLong();
    }

    /**
     * 获取 token 值
     */
    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    /**
     * 获取当前登录账号id, 如果未登录，则返回 null
     */
    public static Object getLoginIdDefaultNull() {
        return stpLogic.getLoginIdDefaultNull();
    }

    /**
     * 查询 Token
     */
    public static java.util.List<String> searchTokenValue(String keyword, int start, int size, boolean sortType) {
        return stpLogic.searchTokenValue(keyword, start, size, sortType);
    }

    /**
     * 根据 Token 强制注销
     */
    public static void kickoutByTokenValue(String tokenValue) {
        stpLogic.kickoutByTokenValue(tokenValue);
    }

    /**
     * 根据 Token 获取 LoginId
     */
    public static Object getLoginIdByToken(String tokenValue) {
        return stpLogic.getLoginIdByToken(tokenValue);
    }

}
