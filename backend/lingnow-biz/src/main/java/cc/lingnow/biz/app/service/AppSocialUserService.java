package cc.lingnow.biz.app.service;

import cc.lingnow.biz.app.entity.AppSocialUser;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * App社交账号绑定服务接口
 *
 * @author LingNow Team
 */
public interface AppSocialUserService extends IService<AppSocialUser> {

    /**
     * 根据OpenID查询
     */
    AppSocialUser getByOpenId(String provider, String openId);

    /**
     * 根据用户ID查询绑定列表
     */
    List<AppSocialUser> getByUserId(Long userId);
}
