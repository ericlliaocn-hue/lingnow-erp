package cc.lingnow.biz.job.entity;

import cc.lingnow.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 定时任务定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJob extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long jobId;

    private String jobName;

    private String jobGroup;

    private String invokeTarget;

    private String cronExpression;

    private String misfirePolicy;

    private String concurrent;

    private Integer status;

    private String remark;
}
