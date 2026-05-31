package cc.lingnow.biz.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cc.lingnow.common.entity.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 错误日志实体
 *
 * @author LingNow Team
 */
@Data
@TableName("sys_error_log")
public class SysErrorLog extends BaseEntity implements Serializable {

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
     * 请求方式
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 堆栈信息
     */
    private String errorStack;

}
