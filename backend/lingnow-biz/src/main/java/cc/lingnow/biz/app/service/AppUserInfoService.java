package cc.lingnow.biz.app.service;

import cc.lingnow.biz.app.entity.AppUserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * App用户扩展信息服务接口
 *
 * @author LingNow Team
 */
public interface AppUserInfoService extends IService<AppUserInfo> {

    /**
     * 根据用户ID查询扩展信息
     */
    AppUserInfo getByUserId(Long userId);
}
