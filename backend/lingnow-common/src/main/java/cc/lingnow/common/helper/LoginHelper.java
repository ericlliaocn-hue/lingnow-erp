package cc.lingnow.common.helper;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotWebContextException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cc.lingnow.common.enums.DeviceType;
import cc.lingnow.common.enums.UserType;
import cc.lingnow.common.model.LoginUser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录鉴权助手
 *
 * @author LingNow Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class LoginHelper {

    public static final String LOGIN_USER_KEY = "loginUser";
    public static final String USER_KEY = "userId";

    /**
     * 登录系统
     *
     * @param loginUser 登录用户信息
     */
    public static void login(LoginUser loginUser) {
        loginByDevice(loginUser, null);
    }

    /**
     * 登录系统 基于 设备类型
     *
     * @param loginUser 登录用户信息
     */
    public static void loginByDevice(LoginUser loginUser, DeviceType deviceType) {
        SaStorage storage = SaHolder.getStorage();
        storage.set(LOGIN_USER_KEY, loginUser);
        storage.set(USER_KEY, loginUser.getUserId());

        SaLoginModel model = new SaLoginModel();
        if (ObjectUtil.isNotNull(deviceType)) {
            model.setDevice(deviceType.getDevice());
        }

        // 调用 StpUtil 进行登录
        StpUtil.login(loginUser.getUserId(), model.setExtra(USER_KEY, loginUser.getUserId()));

        // 确保用户信息存储到 tokenSession
        SaSession tokenSession = StpUtil.getTokenSession();
        if (tokenSession != null) {
            tokenSession.set(LOGIN_USER_KEY, loginUser);
        } else {
            log.error("登录时无法获取TokenSession，loginId={}", loginUser.getUserId());
        }
    }

    /**
     * 获取用户(多级缓存)
     */
    public static LoginUser getLoginUser() {
        try {
            // 1. 尝试从线程存储获取
            SaStorage storage = SaHolder.getStorage();
            LoginUser loginUser = (LoginUser) storage.get(LOGIN_USER_KEY);
            if (loginUser != null) {
                return loginUser;
            }

            // 2. 安全方式检查登录状态
            if (!isLoginSafely()) {
                throw new NotLoginException("未登录或登录已失效", "APP_USER", "app");
            }

            // 3. 安全获取TokenSession
            SaSession tokenSession = StpUtil.getTokenSession();
            if (tokenSession == null) {
                log.error("TokenSession不存在，loginId={}", StpUtil.getLoginId());
                throw new NotLoginException("登录会话不存在", "APP_USER", "app");
            }

            loginUser = (LoginUser) tokenSession.get(LOGIN_USER_KEY);
            if (loginUser == null) {
                // 有可能 token 有效但是 session 里没数据（比如以前的 token）
                // 这里可以做一个降级，比如只返回 userId 或者触发强制登出
                // 但为了严谨，抛出异常要求重新登录
                log.error("登录用户数据不存在，loginId={}, token={}",
                        StpUtil.getLoginId(), StpUtil.getTokenValue());
                throw new NotLoginException("登录用户数据不存在", "APP_USER", "app");
            }

            // 4. 缓存到线程存储
            storage.set(LOGIN_USER_KEY, loginUser);
            return loginUser;
        } catch (NotLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取登录用户失败", e);
            throw new NotLoginException("获取用户信息失败", "APP_USER", "app");
        }
    }

    /**
     * 检查是否在Web环境中
     */
    public static boolean isWebContext() {
        try {
            SaHolder.getStorage();
            return true;
        } catch (NotWebContextException e) {
            return false;
        } catch (Exception e) {
            log.debug("检查Web上下文时发生异常", e);
            return false;
        }
    }

    /**
     * 安全方式检查登录状态（不依赖Web上下文）
     */
    public static boolean isLoginSafely() {
        try {
            String tokenValue = StpUtil.getTokenValue();
            if (tokenValue == null) {
                return false;
            }
            Object loginId = StpUtil.getLoginIdByToken(tokenValue);
            return loginId != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取用户基于token
     */
    public static LoginUser getLoginUser(String token) {
        SaSession session = StpUtil.getTokenSessionByToken(token);
        if (session == null) {
            return null;
        }
        return (LoginUser) session.get(LOGIN_USER_KEY);
    }

    /**
     * 获取用户id
     */
    public static Long getUserId() {
        return getUserId(getLoginUser());
    }

    public static Long getUserId(LoginUser loginUser) {
        return loginUser != null ? loginUser.getUserId() : null;
    }

    /**
     * 获取用户账户
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取用户类型
     */
    public static UserType getUserType() {
        // 项目目前可能还没严格区分 UserType 在 session 中的存储
        // 这里只是演示，实际可以从 LoginUser 获取
        return UserType.APP;
    }
}
