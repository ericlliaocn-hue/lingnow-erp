package cc.lingnow.biz.post.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.post.entity.SysUserPost;
import cc.lingnow.biz.post.mapper.SysUserPostMapper;
import cc.lingnow.biz.post.service.SysUserPostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户与岗位关联服务实现类
 *
 * @author LingNow Team
 */
@Service
public class SysUserPostServiceImpl extends ServiceImpl<SysUserPostMapper, SysUserPost> implements SysUserPostService {

    @Override
    public List<Long> selectPostIdsByUserId(Long userId) {
        List<SysUserPost> list = baseMapper.selectList(new LambdaQueryWrapper<SysUserPost>()
                .eq(SysUserPost::getUserId, userId));
        return list.stream().map(SysUserPost::getPostId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPosts(Long userId, List<Long> postIds) {
        // 删除原有岗位
        baseMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));

        if (CollUtil.isNotEmpty(postIds)) {
            List<SysUserPost> list = new ArrayList<>();
            for (Long postId : postIds) {
                SysUserPost up = new SysUserPost();
                up.setUserId(userId);
                up.setPostId(postId);
                list.add(up);
            }
            saveBatch(list);
        }
    }
}
