package cc.lingnow.biz.app.service.impl;

import cc.lingnow.biz.app.entity.AppUser;
import cc.lingnow.biz.app.mapper.AppUserMapper;
import cc.lingnow.biz.app.service.AppUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * App用户服务实现
 *
 * @author LingNow Team
 */
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    @Override
    public AppUser getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, username));
    }

    @Override
    public AppUser getByPhone(String phone) {
        return this.getOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPhone, phone));
    }
}
