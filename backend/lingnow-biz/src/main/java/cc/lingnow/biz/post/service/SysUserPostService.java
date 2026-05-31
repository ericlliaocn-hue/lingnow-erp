package cc.lingnow.biz.post.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.post.entity.SysUserPost;

import java.util.List;

/**
 * 用户与岗位关联服务接口
 *
 * @author LingNow Team
 */
public interface SysUserPostService extends IService<SysUserPost> {

    /**
     * 根据用户ID查询岗位ID列表
     *
     * @param userId 用户ID
     * @return 岗位ID列表
     */
    List<Long> selectPostIdsByUserId(Long userId);

    /**
     * 为用户分配岗位
     *
     * @param userId  用户ID
     * @param postIds 岗位ID列表
     */
    void assignPosts(Long userId, List<Long> postIds);
}
