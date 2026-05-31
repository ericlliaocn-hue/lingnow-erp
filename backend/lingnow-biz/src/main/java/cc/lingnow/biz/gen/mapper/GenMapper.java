package cc.lingnow.biz.gen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.lingnow.biz.gen.bo.GenTableQueryBO;
import cc.lingnow.biz.gen.entity.GenTable;
import cc.lingnow.biz.gen.entity.GenTableColumn;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 代码生成 数据层
 *
 * @author lingnow
 */
@Mapper
public interface GenMapper extends BaseMapper<GenTable> {
    /**
     * 查询据库列表
     *
     * @param genTableQueryBO 业务查询参数
     * @return 数据库表集合
     */
    List<GenTable> selectDbTableList(GenTableQueryBO genTableQueryBO);

    /**
     * 查询据库列表
     *
     * @param tableNames 表名称组
     * @return 数据库表集合
     */
    List<GenTable> selectDbTableListByNames(String[] tableNames);

    /**
     * 查询所有表信息
     *
     * @return 表信息集合
     */
    List<GenTable> selectGenTableAll();

    /**
     * 查询表ID业务信息
     *
     * @param id 业务ID
     * @return 业务信息
     */
    GenTable selectGenTableById(Long id);

    /**
     * 查询表名称业务信息
     *
     * @param tableName 表名称
     * @return 业务信息
     */
    GenTable selectGenTableByName(String tableName);

    /**
     * 查询列信息
     *
     * @param tableName 表名称
     * @return 列信息
     */
    List<GenTableColumn> selectDbTableColumnsByName(String tableName);
}
