package cc.lingnow.biz.post.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.post.entity.SysPost;

import java.util.List;

/**
 * 岗位Service接口
 *
 * @author LingNow Team
 */
public interface SysPostService extends IService<SysPost> {

    /**
     * 查询岗位列表
     *
     * @param post 岗位信息
     * @return 岗位集合
     */
    List<SysPost> selectPostList(SysPost post);

    /**
     * 校验岗位名称
     */
    boolean checkPostNameUnique(SysPost post);

    /**
     * 校验岗位编码
     */
    boolean checkPostCodeUnique(SysPost post);
}
