package cc.lingnow.biz.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cc.lingnow.common.entity.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 慢SQL日志实体
 *
 * @author LingNow Team
 */
@Data
@TableName("sys_slow_sql_log")
public class SysSlowSqlLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 执行时长(ms)
     */
    private Long executionTime;

    /**
     * SQL语句
     */
    private String sqlStatement;

}
