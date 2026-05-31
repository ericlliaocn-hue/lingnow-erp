package cc.lingnow.biz.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.post.entity.SysPost;
import cc.lingnow.biz.post.mapper.SysPostMapper;
import cc.lingnow.biz.post.service.SysPostService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 岗位Service业务层处理
 *
 * @author LingNow Team
 */
@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements SysPostService {

    @Override
    public List<SysPost> selectPostList(SysPost post) {
        LambdaQueryWrapper<SysPost> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SysPost::getDelFlag, false)
                .like(ObjectUtils.isNotEmpty(post.getPostCode()), SysPost::getPostCode, post.getPostCode())
                .like(ObjectUtils.isNotEmpty(post.getPostName()), SysPost::getPostName, post.getPostName())
                .eq(post.getStatus() != null, SysPost::getStatus, post.getStatus())
                .eq(post.getDeptId() != null, SysPost::getDeptId, post.getDeptId())
                .orderByAsc(SysPost::getPostSort);
        return baseMapper.selectList(lqw);
    }

    @Override
    public boolean checkPostNameUnique(SysPost post) {
        Long postId = ObjectUtils.isEmpty(post.getPostId()) ? -1L : post.getPostId();
        SysPost info = baseMapper.selectOne(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getPostName, post.getPostName())
                .last("limit 1"));
        return ObjectUtils.isEmpty(info) || info.getPostId().longValue() == postId.longValue();
    }

    @Override
    public boolean checkPostCodeUnique(SysPost post) {
        Long postId = ObjectUtils.isEmpty(post.getPostId()) ? -1L : post.getPostId();
        SysPost info = baseMapper.selectOne(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getPostCode, post.getPostCode())
                .last("limit 1"));
        return ObjectUtils.isEmpty(info) || info.getPostId().longValue() == postId.longValue();
    }
}
