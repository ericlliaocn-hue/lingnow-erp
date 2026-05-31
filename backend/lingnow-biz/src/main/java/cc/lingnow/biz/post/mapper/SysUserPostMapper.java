package cc.lingnow.biz.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.post.entity.SysUserPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户与岗位关联 Mapper 接口
 *
 * @author LingNow Team
 */
@Mapper
public interface SysUserPostMapper extends BaseMapper<SysUserPost> {
}
