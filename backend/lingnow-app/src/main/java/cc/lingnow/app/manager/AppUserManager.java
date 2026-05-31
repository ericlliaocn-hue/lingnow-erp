package cc.lingnow.app.manager;

import cc.lingnow.app.model.bo.*;
import cc.lingnow.app.model.vo.AppUserVO;
import cc.lingnow.biz.app.entity.AppUser;
import cc.lingnow.biz.app.entity.AppUserInfo;
import cc.lingnow.biz.app.service.AppUserInfoService;
import cc.lingnow.biz.app.service.AppUserService;
import cc.lingnow.biz.config.service.SysConfigService;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.helper.LoginHelper;
import cc.lingnow.common.model.LoginUser;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppUserManager {

    private static final String VERIFY_CODE_KEY_PREFIX = "app:sms:code:";

    private final AppUserService appUserService;
    private final AppUserInfoService appUserInfoService;
    private final SysConfigService sysConfigService;
    private final RedisTemplate<String, Object> redisTemplate;

    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    public AppUserVO login(AppLoginBO loginBO) {
        AppUser user = null;
        if ("password".equals(loginBO.getType())) {
            // 账号密码登录 (支持手机号/用户名)
            user = appUserService.getOne(new LambdaQueryWrapper<AppUser>()
//                    .eq(AppUser::getUsername, loginBO.getAccount())
//                    .or()
                    .eq(AppUser::getPhone, loginBO.getAccount()));

            Assert.notNull(user, "账号不存在");

            // 验证密码
            if (!BCrypt.checkpw(loginBO.getCredential(), user.getPassword())) {
                throw new BusinessException(ErrorCode.PASSWORD_ERROR);
            }
        } else if ("code".equals(loginBO.getType())) {
            verifyCode(loginBO.getAccount(), loginBO.getCredential(), true);

            user = appUserService.getOne(new LambdaQueryWrapper<AppUser>()
                    .eq(AppUser::getPhone, loginBO.getAccount()));

            if (user == null) {
                // 检查全局配置是否允许自动注册
                String autoRegister = sysConfigService.selectConfigByKey("sys.user.auto_register");
                // 默认开启 (true 或 空)
                boolean isAutoRegister = "".equals(autoRegister) || "true".equals(autoRegister);
                Assert.isTrue(isAutoRegister, "手机号未注册");

                // 自动注册
                user = new AppUser();
                user.setUsername(loginBO.getAccount()); // 默认用户名为手机号
                user.setPhone(loginBO.getAccount());
                user.setPassword(BCrypt.hashpw(UUID.randomUUID().toString()));
                user.setStatus(CommonConstants.STATUS_NORMAL); // 正常
                appUserService.save(user);

                // 创建扩展信息
                AppUserInfo userInfo = new AppUserInfo();
                userInfo.setUserId(user.getUserId());
                appUserInfoService.save(userInfo);

            }
        }

        Assert.notNull(user, "登录失败");

        // 构建 LoginUser
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setUserType("APP");
        loginUser.setLoginTime(System.currentTimeMillis());

        // Sa-Token 登录 (使用 LoginHelper 统一处理 Session)
        LoginHelper.login(loginUser);

        return AppUserVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(desensitizePhone(user.getPhone()))
                .avatar(user.getAvatar())
                .token(StpUtil.getTokenValue())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public AppUserVO register(AppRegisterBO registerBO) {
        verifyCode(registerBO.getPhone(), registerBO.getCode(), true);

        // 检查手机号是否已存在
        long count = appUserService.count(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPhone, registerBO.getPhone()));
        Assert.isTrue(count == 0, "手机号已注册");

        // 1. 创建基础用户
        AppUser user = new AppUser();
        user.setUsername(registerBO.getPhone()); // 默认用户名为手机号
        user.setPhone(registerBO.getPhone());
        user.setPassword(BCrypt.hashpw(registerBO.getPassword())); // 加密密码
        user.setStatus(CommonConstants.STATUS_NORMAL); // 正常
        appUserService.save(user);

        // 2. 创建扩展信息
        AppUserInfo userInfo = new AppUserInfo();
        userInfo.setUserId(user.getUserId());
        appUserInfoService.save(userInfo);

        // 构建 LoginUser
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setUserType("APP");
        loginUser.setLoginTime(System.currentTimeMillis());

        // Sa-Token 登录 (使用 LoginHelper 统一处理 Session)
        LoginHelper.login(loginUser);

        return AppUserVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(desensitizePhone(user.getPhone()))
                .avatar(user.getAvatar())
                .token(StpUtil.getTokenValue())
                .build();
    }

    public AppUserVO getProfile(Long userId) {
        AppUser user = appUserService.getById(userId);

        return AppUserVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(desensitizePhone(user.getPhone()))
                .avatar(user.getAvatar())
                // 扩展信息需要 VO 支持，目前 VO 只有基础信息，如果 VO 扩展了再加
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, AppUserProfileUpdateBO updateBO) {
        // 更新基础信息
        AppUser user = new AppUser();
        user.setUserId(userId);
        boolean updateBasic = false;

        if (updateBO.getNickname() != null) {
            user.setNickname(updateBO.getNickname());
            updateBasic = true;
        }
        if (updateBO.getAvatar() != null) {
            user.setAvatar(updateBO.getAvatar());
            updateBasic = true;
        }

        if (updateBasic) {
            appUserService.updateById(user);
        }

        // 更新扩展信息
        AppUserInfo userInfo = new AppUserInfo();
        // 需要先查一下是否存在，或者直接 updateWrapper
        AppUserInfo existInfo = appUserInfoService.getByUserId(userId);
        if (existInfo == null) {
            userInfo.setUserId(userId);
            // 填充其他字段
            Optional.ofNullable(updateBO.getGender()).ifPresent(userInfo::setGender);
            Optional.ofNullable(updateBO.getRegion()).ifPresent(userInfo::setRegion);
            // 生日转换逻辑略
            appUserInfoService.save(userInfo);
        } else {
            userInfo.setUserId(userId);
            boolean updateInfo = false;
            if (updateBO.getGender() != null) {
                userInfo.setGender(updateBO.getGender());
                updateInfo = true;
            }
            if (updateBO.getRegion() != null) {
                userInfo.setRegion(updateBO.getRegion());
                updateInfo = true;
            }
            if (updateInfo) {
                appUserInfoService.updateById(userInfo);
            }
        }
    }

    public void forgetPassword(String phone, String code, String newPassword) {
        verifyCode(phone, code, true);

        AppUser user = appUserService.getOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPhone, phone));
        Assert.notNull(user, "手机号未注册");

        user.setPassword(BCrypt.hashpw(newPassword)); // 加密
        appUserService.updateById(user);
    }

    public void changePassword(Long userId, AppPasswordChangeBO changeBO) {
        AppUser user = appUserService.getById(userId);
        Assert.notNull(user, "用户不存在");

        // 验证旧密码
        if (!BCrypt.checkpw(changeBO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 修改密码
        user.setPassword(BCrypt.hashpw(changeBO.getNewPassword()));
        appUserService.updateById(user);

        // 强制下线
        StpUtil.logout(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePhone(Long userId, AppPhoneChangeBO changeBO) {
        AppUser currentUser = appUserService.getById(userId);
        Assert.notNull(currentUser, "用户不存在");

        verifyCode(currentUser.getPhone(), changeBO.getOldCode(), true);
        verifyCode(changeBO.getNewPhone(), changeBO.getNewCode(), true);

        long count = appUserService.count(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPhone, changeBO.getNewPhone())
                .ne(AppUser::getUserId, userId)); // 排除自己
        Assert.isTrue(count == 0, "新手机号已注册");

        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setPhone(changeBO.getNewPhone());
        appUserService.updateById(user);
    }

    public void validateCode(String phone, String code) {
        verifyCode(phone, code, false);
    }

    private void verifyCode(String phone, String code, boolean consume) {
        String key = VERIFY_CODE_KEY_PREFIX + phone;
        Object cachedCode = redisTemplate.opsForValue().get(key);
        if (cachedCode == null || !Objects.equals(String.valueOf(cachedCode), code)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }
        if (consume) {
            redisTemplate.delete(key);
        }
    }
}
