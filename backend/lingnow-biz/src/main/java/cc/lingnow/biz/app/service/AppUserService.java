package cc.lingnow.biz.app.service;

import cc.lingnow.biz.app.entity.AppUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * App用户服务接口
 *
 * @author LingNow Team
 */
public interface AppUserService extends IService<AppUser> {

    /**
     * 根据用户名查询
     */
    AppUser getByUsername(String username);

    /**
     * 根据手机号查询
     */
    AppUser getByPhone(String phone);
}
