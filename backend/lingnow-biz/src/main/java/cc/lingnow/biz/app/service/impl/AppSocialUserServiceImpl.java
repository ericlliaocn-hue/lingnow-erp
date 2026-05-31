package cc.lingnow.biz.app.service.impl;

import cc.lingnow.biz.app.entity.AppSocialUser;
import cc.lingnow.biz.app.mapper.AppSocialUserMapper;
import cc.lingnow.biz.app.service.AppSocialUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * App社交账号绑定服务实现
 *
 * @author LingNow Team
 */
@Service
public class AppSocialUserServiceImpl extends ServiceImpl<AppSocialUserMapper, AppSocialUser> implements AppSocialUserService {

    @Override
    public AppSocialUser getByOpenId(String provider, String openId) {
        return this.getOne(new LambdaQueryWrapper<AppSocialUser>()
                .eq(AppSocialUser::getProvider, provider)
                .eq(AppSocialUser::getOpenId, openId));
    }

    @Override
    public List<AppSocialUser> getByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<AppSocialUser>()
                .eq(AppSocialUser::getUserId, userId));
    }
}
