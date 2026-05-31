package cc.lingnow.biz.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.user.entity.SysSocialUser;

import java.util.List;

/**
 * 社交账号绑定 Service 接口
 *
 * @author LingNow Team
 */
public interface SysSocialUserService extends IService<SysSocialUser> {

    /**
     * 根据OpenID查询
     *
     * @param provider 平台
     * @param openId   OpenID
     * @return 绑定信息
     */
    SysSocialUser getByOpenId(String provider, String openId);

    /**
     * 根据用户ID查询绑定列表
     *
     * @param userId 用户ID
     * @return 绑定列表
     */
    List<SysSocialUser> getByUserId(Long userId);
}
