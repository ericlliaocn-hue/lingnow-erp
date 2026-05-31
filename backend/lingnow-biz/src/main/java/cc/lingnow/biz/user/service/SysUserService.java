package cc.lingnow.biz.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.user.entity.SysUser;

/**
 * 用户服务接口
 *
 * @author LingNow Team
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser getByUsername(String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    SysUser getByPhone(String phone);

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param phone    手机号
     * @return 注册的用户
     */
    SysUser register(String username, String password, String phone);

}
