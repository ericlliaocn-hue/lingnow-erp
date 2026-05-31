package cc.lingnow.biz.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cc.lingnow.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志记录
 *
 * @author LingNow Team
 */
@Data
@TableName("sys_oper_log")
@Schema(description = "操作日志记录")
public class SysOperLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "日志主键")
    private Long operId;

    /**
     * 模块标题
     */
    @Schema(description = "模块标题")
    private String title;

    /**
     * 业务类型（0其它 1新增 2修改 3删除）
     */
    @Schema(description = "业务类型（0其它 1新增 2修改 3删除）")
    private Integer businessType;

    /**
     * 方法名称
     */
    @Schema(description = "方法名称")
    private String method;

    /**
     * 请求方式
     */
    @Schema(description = "请求方式")
    private String requestMethod;

    /**
     * 操作类别（0其它 1后台用户 2手机端用户）
     */
    @Schema(description = "操作类别（0其它 1后台用户 2手机端用户）")
    private Integer operatorType;

    /**
     * 操作人员
     */
    @Schema(description = "操作人员")
    private String operName;

    /**
     * 部门名称
     */
    @Schema(description = "部门名称")
    private String deptName;

    /**
     * 请求URL
     */
    @Schema(description = "请求URL")
    private String operUrl;

    /**
     * 主机地址
     */
    @Schema(description = "主机地址")
    private String operIp;

    /**
     * 操作地点
     */
    @Schema(description = "操作地点")
    private String operLocation;

    /**
     * 请求参数
     */
    @Schema(description = "请求参数")
    private String operParam;

    /**
     * 返回参数
     */
    @Schema(description = "返回参数")
    private String jsonResult;

    /**
     * 操作状态（1正常 0异常）
     */
    @Schema(description = "操作状态（1正常 0异常）")
    private Integer status;

    /**
     * 错误消息
     */
    @Schema(description = "错误消息")
    private String errorMsg;

    /**
     * 操作时间
     */
    @Schema(description = "操作时间")
    private LocalDateTime operTime;

    /**
     * 消耗时间
     */
    @Schema(description = "消耗时间")
    private Long costTime;
}
