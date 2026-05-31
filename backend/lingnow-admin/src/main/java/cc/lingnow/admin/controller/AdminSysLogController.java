package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.AdminSysLogManager;
import cc.lingnow.admin.model.bo.ErrorLogQueryBO;
import cc.lingnow.admin.model.bo.LoginLogQueryBO;
import cc.lingnow.admin.model.bo.OperLogQueryBO;
import cc.lingnow.admin.model.bo.SlowSqlLogQueryBO;
import cc.lingnow.admin.model.vo.ErrorLogVO;
import cc.lingnow.admin.model.vo.LoginLogVO;
import cc.lingnow.admin.model.vo.OperLogVO;
import cc.lingnow.admin.model.vo.SlowSqlLogVO;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统日志管理控制器
 *
 * @author LingNow Team
 */
@RestController
@RequestMapping("/sys/log")
@RequiredArgsConstructor
@Tag(name = "系统日志管理")
public class AdminSysLogController {

    private final AdminSysLogManager adminSysLogManager;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/oper/list")
    public Result<PageResult<OperLogVO>> listOperLogs(@Valid OperLogQueryBO query) {
        PageResult<OperLogVO> result = adminSysLogManager.listOperLogs(query);
        return Result.success(result);
    }

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/login/list")
    public Result<PageResult<LoginLogVO>> listLoginLogs(@Valid LoginLogQueryBO query) {
        PageResult<LoginLogVO> result = adminSysLogManager.listLoginLogs(query);
        return Result.success(result);
    }

    @Operation(summary = "分页查询错误日志")
    @GetMapping("/error/list")
    public Result<PageResult<ErrorLogVO>> listErrorLogs(@Valid ErrorLogQueryBO query) {
        PageResult<ErrorLogVO> result = adminSysLogManager.listErrorLogs(query);
        return Result.success(result);
    }

    @Operation(summary = "分页查询慢SQL日志")
    @GetMapping("/slowSql/list")
    public Result<PageResult<SlowSqlLogVO>> listSlowSqlLogs(@Valid SlowSqlLogQueryBO query) {
        PageResult<SlowSqlLogVO> result = adminSysLogManager.listSlowSqlLogs(query);
        return Result.success(result);
    }
}
