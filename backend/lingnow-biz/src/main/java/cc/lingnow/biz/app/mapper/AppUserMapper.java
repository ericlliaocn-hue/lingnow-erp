package cc.lingnow.biz.app.mapper;

import cc.lingnow.biz.app.entity.AppUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * App用户 Mapper
 *
 * @author LingNow Team
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
