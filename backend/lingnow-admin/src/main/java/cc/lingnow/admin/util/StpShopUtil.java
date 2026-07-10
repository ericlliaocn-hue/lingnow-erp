package cc.lingnow.admin.util;

import cn.dev33.satoken.stp.StpLogic;

/**
 * H5 客户下单端认证工具。
 */
public class StpShopUtil {

    public static final String TYPE = "shop";

    public static StpLogic stpLogic = new StpLogic(TYPE);

    public static void login(Object id) {
        stpLogic.login(id);
    }

    public static void logout() {
        stpLogic.logout();
    }

    public static void checkLogin() {
        stpLogic.checkLogin();
    }

    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    public static Long getLoginIdAsLong() {
        return stpLogic.getLoginIdAsLong();
    }

    public static Object getLoginIdDefaultNull() {
        return stpLogic.getLoginIdDefaultNull();
    }

    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }
}
