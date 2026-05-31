package cc.lingnow.biz.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.file.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息Mapper接口
 *
 * @author LingNow Team
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
}
