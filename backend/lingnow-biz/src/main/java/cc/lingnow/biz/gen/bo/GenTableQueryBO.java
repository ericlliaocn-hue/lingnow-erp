package cc.lingnow.biz.gen.bo;

import lombok.Data;

/**
 * 代码生成查询参数
 *
 * @author lingnow
 */
@Data
public class GenTableQueryBO {

    /**
     * 表名称
     */
    private String tableName;

    /**
     * 表描述
     */
    private String tableComment;

    /**
     * 开始时间
     */
    private String beginTime;

    /**
     * 结束时间
     */
    private String endTime;
}
