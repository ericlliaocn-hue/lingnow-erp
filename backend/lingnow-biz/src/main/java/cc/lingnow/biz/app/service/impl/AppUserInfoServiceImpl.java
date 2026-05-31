package cc.lingnow.biz.app.service.impl;

import cc.lingnow.biz.app.entity.AppUserInfo;
import cc.lingnow.biz.app.mapper.AppUserInfoMapper;
import cc.lingnow.biz.app.service.AppUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * App用户扩展信息服务实现
 *
 * @author LingNow Team
 */
@Service
public class AppUserInfoServiceImpl extends ServiceImpl<AppUserInfoMapper, AppUserInfo> implements AppUserInfoService {

    @Override
    public AppUserInfo getByUserId(Long userId) {
        return this.getById(userId);
    }
}
