package cc.lingnow.admin.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cc.lingnow.admin.model.bo.PostQueryBO;
import cc.lingnow.admin.model.bo.PostSaveBO;
import cc.lingnow.admin.model.vo.DeptSimpleVO;
import cc.lingnow.admin.model.vo.PostVO;
import cc.lingnow.biz.dept.service.SysDeptService;
import cc.lingnow.biz.post.entity.SysPost;
import cc.lingnow.biz.post.service.SysPostService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 岗位管理业务逻辑
 *
 * @author LingNow Team
 */
@Service
@RequiredArgsConstructor
public class SysPostManager {

    private final SysPostService postService;
    private final SysDeptService deptService;

    /**
     * 查询岗位列表
     */
    public List<PostVO> listPosts(PostQueryBO query) {
        SysPost post = BeanUtil.copyProperties(query, SysPost.class);
        List<SysPost> list = postService.selectPostList(post);

        // 获取部门名称
        if (CollUtil.isNotEmpty(list)) {
            List<Long> deptIds = list.stream()
                    .map(SysPost::getDeptId)
                    .filter(ObjectUtil::isNotNull)
                    .distinct()
                    .collect(Collectors.toList());

            List<DeptSimpleVO> deptList = new ArrayList<>();
            if (CollUtil.isNotEmpty(deptIds)) {
                deptList = deptService.listByIds(deptIds).stream()
                        .map(dept -> DeptSimpleVO.builder()
                                .deptId(dept.getDeptId())
                                .deptName(dept.getDeptName())
                                .build())
                        .collect(Collectors.toList());
            }

            List<DeptSimpleVO> finalDeptList = deptList;
            return list.stream()
                    .map(item -> {
                        PostVO vo = BeanUtil.copyProperties(item, PostVO.class);
                        if (item.getDeptId() != null) {
                            finalDeptList.stream()
                                    .filter(d -> d.getDeptId().equals(item.getDeptId()))
                                    .findFirst()
                                    .ifPresent(d -> vo.setDeptName(d.getDeptName()));
                        }
                        return vo;
                    })
                    .collect(Collectors.toList());
        }

        return list.stream()
                .map(item -> BeanUtil.copyProperties(item, PostVO.class))
                .collect(Collectors.toList());
    }

    /**
     * 查询岗位详情
     */
    public PostVO getPost(Long postId) {
        SysPost post = postService.getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return BeanUtil.copyProperties(post, PostVO.class);
    }

    /**
     * 新增岗位
     */
    public void addPost(PostSaveBO bo) {
        SysPost post = BeanUtil.copyProperties(bo, SysPost.class);
        if (!postService.checkPostNameUnique(post)) {
            throw new BusinessException(ErrorCode.POST_NAME_EXIST);
        }
        if (!postService.checkPostCodeUnique(post)) {
            throw new BusinessException(ErrorCode.POST_CODE_EXIST);
        }
        postService.save(post);
    }

    /**
     * 修改岗位
     */
    public void updatePost(PostSaveBO bo) {
        if (ObjectUtil.isNull(bo.getPostId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        SysPost post = BeanUtil.copyProperties(bo, SysPost.class);
        if (!postService.checkPostNameUnique(post)) {
            throw new BusinessException(ErrorCode.POST_NAME_EXIST);
        }
        if (!postService.checkPostCodeUnique(post)) {
            throw new BusinessException(ErrorCode.POST_CODE_EXIST);
        }
        postService.updateById(post);
    }

    /**
     * 删除岗位
     */
    public void removePost(List<Long> postIds) {
        postService.removeByIds(postIds);
    }
}
