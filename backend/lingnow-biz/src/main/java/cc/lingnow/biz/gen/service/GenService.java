package cc.lingnow.biz.gen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.gen.bo.GenTableQueryBO;
import cc.lingnow.biz.gen.entity.GenTable;

import java.util.List;
import java.util.Map;

/**
 * 代码生成 服务层
 *
 * @author lingnow
 */
public interface GenService extends IService<GenTable> {
    /**
     * 查询据库列表
     *
     * @param genTableQueryBO 业务查询参数
     * @return 数据库表集合
     */
    List<GenTable> selectDbTableList(GenTableQueryBO genTableQueryBO);

    /**
     * 查询业务列表
     *
     * @param genTable 业务信息
     * @return 业务表集合
     */
    List<GenTable> selectGenTableList(GenTable genTable);

    /**
     * 查询业务信息
     *
     * @param id 业务ID
     * @return 业务信息
     */
    GenTable selectGenTableById(Long id);

    /**
     * 修改业务
     *
     * @param genTable 业务信息
     */
    void updateGenTable(GenTable genTable);

    /**
     * 导入表结构
     *
     * @param tableNames 导入表列表
     */
    void importGenTable(String[] tableNames);

    /**
     * 预览代码
     *
     * @param tableName 表名称
     * @return 预览数据
     */
    Map<String, String> previewCode(String tableName);

    /**
     * 生成代码（下载方式）
     *
     * @param tableName 表名称
     * @return 数据
     */
    byte[] downloadCode(String tableName);

    /**
     * 批量生成代码（下载方式）
     *
     * @param tableNames 表数组
     * @return 数据
     */
    byte[] downloadCode(String[] tableNames);

    /**
     * 修改保存参数校验
     *
     * @param genTable 业务信息
     */
    void validateEdit(GenTable genTable);

    /**
     * 同步数据库
     *
     * @param tableName 表名称
     */
    void synchDb(String tableName);

    /**
     * 批量删除业务表
     *
     * @param tableIds 需要删除的表ID
     */
    void deleteGenTableByIds(Long[] tableIds);
}
