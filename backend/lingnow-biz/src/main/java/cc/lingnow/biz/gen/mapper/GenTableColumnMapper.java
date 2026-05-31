package cc.lingnow.biz.gen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.gen.entity.GenTableColumn;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 代码生成业务字段 数据层
 *
 * @author lingnow
 */
@Mapper
public interface GenTableColumnMapper extends BaseMapper<GenTableColumn> {
    /**
     * 根据表名称查询列信息
     *
     * @param tableName 表名称
     * @return 列信息
     */
    List<GenTableColumn> selectDbTableColumnsByName(String tableName);
}
