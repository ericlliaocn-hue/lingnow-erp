package cc.lingnow.biz.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.mapper.SysUserMapper;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 *
 * @author LingNow Team
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final Integer INTERNAL_ACCOUNT_YES = 1;
    private static final Integer INTERNAL_ACCOUNT_NO = 0;

    @Override
    public SysUser getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Override
    public SysUser getByPhone(String phone) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone));
    }

    @Override
    public SysUser register(String username, String password, String phone) {
        // 检查用户名是否已存在
        if (getByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USER_EXIST);
        }

        // 检查手机号是否已存在
        if (getByPhone(phone) != null) {
            throw new BusinessException(ErrorCode.USER_EXIST.getCode(), "手机号已被注册");
        }

        // 创建用户
        SysUser sysUser = new SysUser();
        sysUser.setUsername(username);
        sysUser.setPassword(BCrypt.hashpw(password)); // 使用BCrypt加密密码
        sysUser.setPhone(phone);
        sysUser.setNickname(username);
        sysUser.setStatus(CommonConstants.STATUS_NORMAL);

        this.save(sysUser);
        log.info("用户注册成功: username={}, phone={}", username, phone);

        return sysUser;
    }

    @Override
    public boolean isInternalAccount(SysUser user) {
        return user != null && INTERNAL_ACCOUNT_YES.equals(user.getInternalAccount());
    }

    @Override
    public boolean isSuperAdmin(SysUser user) {
        return user != null && ("admin".equals(user.getUsername()) || isInternalAccount(user));
    }

    @Override
    public LambdaQueryWrapper<SysUser> businessVisibleQuery() {
        return Wrappers.<SysUser>lambdaQuery().eq(SysUser::getInternalAccount, INTERNAL_ACCOUNT_NO);
    }

}
