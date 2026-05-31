package cc.lingnow.biz.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.user.entity.SysSocialUser;
import cc.lingnow.biz.user.mapper.SysSocialUserMapper;
import cc.lingnow.biz.user.service.SysSocialUserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 社交账号绑定 Service 实现
 *
 * @author LingNow Team
 */
@Service
public class SysSocialUserServiceImpl extends ServiceImpl<SysSocialUserMapper, SysSocialUser> implements SysSocialUserService {

    @Override
    public SysSocialUser getByOpenId(String provider, String openId) {
        return this.getOne(new LambdaQueryWrapper<SysSocialUser>()
                .eq(SysSocialUser::getProvider, provider)
                .eq(SysSocialUser::getOpenId, openId));
    }

    @Override
    public List<SysSocialUser> getByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<SysSocialUser>()
                .eq(SysSocialUser::getUserId, userId));
    }
}
