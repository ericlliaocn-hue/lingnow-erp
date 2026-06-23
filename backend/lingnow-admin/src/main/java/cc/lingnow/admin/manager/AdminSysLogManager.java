package cc.lingnow.admin.manager;

import cc.lingnow.admin.model.bo.ErrorLogQueryBO;
import cc.lingnow.admin.model.bo.LoginLogQueryBO;
import cc.lingnow.admin.model.bo.OperLogQueryBO;
import cc.lingnow.admin.model.bo.SlowSqlLogQueryBO;
import cc.lingnow.admin.model.vo.ErrorLogVO;
import cc.lingnow.admin.model.vo.LoginLogVO;
import cc.lingnow.admin.model.vo.OperLogVO;
import cc.lingnow.admin.model.vo.SlowSqlLogVO;
import cc.lingnow.biz.monitor.entity.SysErrorLog;
import cc.lingnow.biz.monitor.entity.SysLoginLog;
import cc.lingnow.biz.monitor.entity.SysOperLog;
import cc.lingnow.biz.monitor.entity.SysSlowSqlLog;
import cc.lingnow.biz.monitor.service.SysErrorLogService;
import cc.lingnow.biz.monitor.service.SysLoginLogService;
import cc.lingnow.biz.monitor.service.SysOperLogService;
import cc.lingnow.biz.monitor.service.SysSlowSqlLogService;
import cc.lingnow.common.vo.PageResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统日志管理 Manager
 *
 * @author LingNow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSysLogManager {

    private final SysOperLogService operLogService;
    private final SysLoginLogService loginLogService;
    private final SysErrorLogService errorLogService;
    private final SysSlowSqlLogService slowSqlLogService;

    /**
     * 分页查询操作日志
     */
    public PageResult<OperLogVO> listOperLogs(OperLogQueryBO query) {
        Page<SysOperLog> page = new Page<>(query.getCurrent(), query.getSize());

        LambdaQueryWrapper<SysOperLog> wrapper = Wrappers.lambdaQuery();
        wrapper.like(ObjUtil.isNotEmpty(query.getTitle()), SysOperLog::getTitle, query.getTitle())
                .like(ObjUtil.isNotEmpty(query.getOperName()), SysOperLog::getOperName, query.getOperName())
                .eq(ObjUtil.isNotEmpty(query.getBusinessType()), SysOperLog::getBusinessType, query.getBusinessType())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), SysOperLog::getStatus, query.getStatus())
                .ge(ObjUtil.isNotEmpty(query.getStartTime()), SysOperLog::getOperTime, query.getStartTime())
                .le(ObjUtil.isNotEmpty(query.getEndTime()), SysOperLog::getOperTime, query.getEndTime())
                .notInSql(SysOperLog::getOperName, "SELECT username FROM sys_user WHERE internal_account = 1")
                .orderByDesc(SysOperLog::getOperId);

        IPage<SysOperLog> result = operLogService.page(page, wrapper);

        List<OperLogVO> voList = result.getRecords().stream()
                .map(log -> BeanUtil.toBean(log, OperLogVO.class))
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    /**
     * 分页查询登录日志
     */
    public PageResult<LoginLogVO> listLoginLogs(LoginLogQueryBO query) {
        Page<SysLoginLog> page = new Page<>(query.getCurrent(), query.getSize());

        LambdaQueryWrapper<SysLoginLog> wrapper = Wrappers.lambdaQuery();
        wrapper.like(ObjUtil.isNotEmpty(query.getUserName()), SysLoginLog::getUserName, query.getUserName())
                .like(ObjUtil.isNotEmpty(query.getIpaddr()), SysLoginLog::getIpaddr, query.getIpaddr())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), SysLoginLog::getStatus, query.getStatus())
                .ge(ObjUtil.isNotEmpty(query.getStartTime()), SysLoginLog::getLoginTime, query.getStartTime())
                .le(ObjUtil.isNotEmpty(query.getEndTime()), SysLoginLog::getLoginTime, query.getEndTime())
                .notInSql(SysLoginLog::getUserName, "SELECT username FROM sys_user WHERE internal_account = 1")
                .orderByDesc(SysLoginLog::getInfoId);

        IPage<SysLoginLog> result = loginLogService.page(page, wrapper);

        List<LoginLogVO> voList = result.getRecords().stream()
                .map(log -> BeanUtil.toBean(log, LoginLogVO.class))
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    /**
     * 分页查询错误日志
     */
    public PageResult<ErrorLogVO> listErrorLogs(ErrorLogQueryBO query) {
        Page<SysErrorLog> page = new Page<>(query.getCurrent(), query.getSize());

        LambdaQueryWrapper<SysErrorLog> wrapper = Wrappers.lambdaQuery();
        wrapper.like(ObjUtil.isNotEmpty(query.getTraceId()), SysErrorLog::getTraceId, query.getTraceId())
                .like(ObjUtil.isNotEmpty(query.getUserName()), SysErrorLog::getUserName, query.getUserName())
                .like(ObjUtil.isNotEmpty(query.getRequestUrl()), SysErrorLog::getRequestUrl, query.getRequestUrl())
                .notInSql(SysErrorLog::getUserName, "SELECT username FROM sys_user WHERE internal_account = 1")
                .orderByDesc(SysErrorLog::getCreateTime);

        IPage<SysErrorLog> result = errorLogService.page(page, wrapper);

        List<ErrorLogVO> voList = result.getRecords().stream()
                .map(log -> BeanUtil.toBean(log, ErrorLogVO.class))
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    /**
     * 分页查询慢SQL日志
     */
    public PageResult<SlowSqlLogVO> listSlowSqlLogs(SlowSqlLogQueryBO query) {
        Page<SysSlowSqlLog> page = new Page<>(query.getCurrent(), query.getSize());

        LambdaQueryWrapper<SysSlowSqlLog> wrapper = Wrappers.lambdaQuery();
        wrapper.like(ObjUtil.isNotEmpty(query.getTraceId()), SysSlowSqlLog::getTraceId, query.getTraceId())
                .like(ObjUtil.isNotEmpty(query.getUserName()), SysSlowSqlLog::getUserName, query.getUserName())
                .ge(ObjUtil.isNotEmpty(query.getMinExecutionTime()), SysSlowSqlLog::getExecutionTime, query.getMinExecutionTime())
                .notInSql(SysSlowSqlLog::getUserName, "SELECT username FROM sys_user WHERE internal_account = 1")
                .orderByDesc(SysSlowSqlLog::getCreateTime);

        IPage<SysSlowSqlLog> result = slowSqlLogService.page(page, wrapper);

        List<SlowSqlLogVO> voList = result.getRecords().stream()
                .map(log -> BeanUtil.toBean(log, SlowSqlLogVO.class))
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }
}
