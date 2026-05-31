package cc.lingnow.biz.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.user.entity.SysSocialUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社交账号绑定 Mapper
 *
 * @author LingNow Team
 */
@Mapper
public interface SysSocialUserMapper extends BaseMapper<SysSocialUser> {
}
