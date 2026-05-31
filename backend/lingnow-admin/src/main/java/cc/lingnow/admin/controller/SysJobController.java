package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.SysJobManager;
import cc.lingnow.admin.model.bo.JobChangeStatusBO;
import cc.lingnow.admin.model.bo.JobLogQueryBO;
import cc.lingnow.admin.model.bo.JobQueryBO;
import cc.lingnow.admin.model.bo.JobSaveBO;
import cc.lingnow.admin.model.vo.JobLogVO;
import cc.lingnow.admin.model.vo.JobVO;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务监控控制器。
 */
@Tag(name = "任务监控", description = "任务监控接口")
@RestController
@RequestMapping("/monitor/job")
@RequiredArgsConstructor
@Validated
public class SysJobController {

    private final SysJobManager jobManager;

    @Operation(summary = "定时任务列表")
    @GetMapping("/list")
    @SaCheckPermission("monitor:job:list")
    public Result<PageResult<JobVO>> list(JobQueryBO query) {
        return Result.success(jobManager.listJobs(query));
    }

    @Operation(summary = "定时任务详情")
    @GetMapping("/{jobId}")
    @SaCheckPermission("monitor:job:list")
    public Result<JobVO> getInfo(@PathVariable Long jobId) {
        return Result.success(jobManager.getJob(jobId));
    }

    @Operation(summary = "新增定时任务")
    @PostMapping
    @SaCheckPermission("monitor:job:add")
    public Result<Void> add(@Valid @RequestBody JobSaveBO bo) {
        jobManager.addJob(bo);
        return Result.success();
    }

    @Operation(summary = "修改定时任务")
    @PutMapping
    @SaCheckPermission("monitor:job:edit")
    public Result<Void> edit(@Valid @RequestBody JobSaveBO bo) {
        jobManager.updateJob(bo);
        return Result.success();
    }

    @Operation(summary = "删除定时任务")
    @DeleteMapping("/{jobIds}")
    @SaCheckPermission("monitor:job:remove")
    public Result<Void> remove(@PathVariable List<Long> jobIds) {
        jobManager.removeJobs(jobIds);
        return Result.success();
    }

    @Operation(summary = "切换任务状态")
    @PutMapping("/changeStatus")
    @SaCheckPermission("monitor:job:changeStatus")
    public Result<Void> changeStatus(@Valid @RequestBody JobChangeStatusBO bo) {
        jobManager.changeStatus(bo);
        return Result.success();
    }

    @Operation(summary = "执行一次")
    @PostMapping("/run/{jobId}")
    @SaCheckPermission("monitor:job:run")
    public Result<Void> run(@PathVariable Long jobId) {
        jobManager.runOnce(jobId);
        return Result.success();
    }

    @Operation(summary = "定时任务日志列表")
    @GetMapping("/log/list")
    @SaCheckPermission("monitor:job:log")
    public Result<PageResult<JobLogVO>> logList(JobLogQueryBO query) {
        return Result.success(jobManager.listJobLogs(query));
    }

    @Operation(summary = "删除定时任务日志")
    @DeleteMapping("/log/{jobLogIds}")
    @SaCheckPermission("monitor:job:logRemove")
    public Result<Void> removeLog(@PathVariable List<Long> jobLogIds) {
        jobManager.removeJobLogs(jobLogIds);
        return Result.success();
    }

    @Operation(summary = "清空定时任务日志")
    @DeleteMapping("/log/clean")
    @SaCheckPermission("monitor:job:logClean")
    public Result<Void> cleanLog() {
        jobManager.cleanJobLogs();
        return Result.success();
    }
}
