package cc.lingnow.admin.controller;

import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.time.Duration;
import java.util.*;

/**
 * 系统监控
 *
 * @author LingNow Team
 */
@Tag(name = "系统监控")
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private DataSource dataSource;
    @Resource
    private Scheduler scheduler;

    @Operation(summary = "在线用户列表")
    @GetMapping("/online/list")
    public Result<Map<String, Object>> list(@RequestParam(value = "username", required = false) String username,
                                            @RequestParam(value = "ipaddr", required = false) String ipaddr) {
        List<Map<String, Object>> userList = getOnlineUsers(username, ipaddr);

        Map<String, Object> result = new HashMap<>();
        result.put("records", userList);
        result.put("total", userList.size());

        return Result.success(result);
    }

    @Operation(summary = "强退用户")
    @DeleteMapping("/online/{tokenId}")
    public Result forceLogout(@PathVariable String tokenId) {
        StpAdminUtil.kickoutByTokenValue(tokenId);
        return Result.success();
    }

    @Operation(summary = "获取缓存监控信息")
    @GetMapping("/cache")
    public Result<Map<String, Object>> getCacheInfo() {
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) RedisServerCommands::info);
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info("commandstats"));
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) RedisServerCommands::dbSize);

        Map<String, Object> result = new HashMap<>(3);
        result.put("info", info);
        result.put("dbSize", dbSize);

        List<Map<String, String>> pieList = new ArrayList<>();
        if (commandStats != null) {
            commandStats.stringPropertyNames().forEach(key -> {
                Map<String, String> data = new HashMap<>(2);
                String property = commandStats.getProperty(key);
                data.put("name", StrUtil.removePrefix(key, "cmdstat_"));
                data.put("value", StrUtil.subBetween(property, "calls=", ",usec"));
                pieList.add(data);
            });
        }
        result.put("commandStats", pieList);
        return Result.success(result);
    }

    @Operation(summary = "获取服务监控数据")
    @GetMapping("/admin/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMxBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMxBean.getNonHeapMemoryUsage();
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long heapUsed = heap.getUsed();
        long heapMax = heap.getMax();
        long nonHeapUsed = nonHeap.getUsed();
        File root = new File(System.getProperty("user.dir"));
        long diskTotal = root.getTotalSpace();
        long diskFree = root.getFreeSpace();
        long diskUsed = Math.max(diskTotal - diskFree, 0);
        Map<String, Object> redisHealth = getRedisHealth();
        Map<String, Object> databaseHealth = getDatabaseHealth();
        Map<String, Object> schedulerHealth = getSchedulerHealth();

        data.put("serviceStatus", allUp(databaseHealth, redisHealth, schedulerHealth) ? "UP" : "WARN");
        data.put("serviceName", "lingnow-admin");
        data.put("uptimeMs", uptimeMs);
        data.put("uptimeText", formatDuration(uptimeMs));
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("processId", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        data.put("availableProcessors", runtime.availableProcessors());
        data.put("systemLoadAverage", osMxBean.getSystemLoadAverage());

        data.put("heapUsed", heapUsed);
        data.put("heapMax", heapMax);
        data.put("heapUsage", percent(heapUsed, heapMax));
        data.put("nonHeapUsed", nonHeapUsed);
        data.put("threadCount", threadMxBean.getThreadCount());
        data.put("daemonThreadCount", threadMxBean.getDaemonThreadCount());
        data.put("peakThreadCount", threadMxBean.getPeakThreadCount());
        data.put("diskUsed", diskUsed);
        data.put("diskTotal", diskTotal);
        data.put("diskUsage", percent(diskUsed, diskTotal));
        data.put("database", databaseHealth);
        data.put("redis", redisHealth);
        data.put("scheduler", schedulerHealth);

        return Result.success(data);
    }

    private Map<String, Object> getDatabaseHealth() {
        Map<String, Object> data = new HashMap<>();
        long start = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            data.put("status", valid ? "UP" : "DOWN");
            data.put("responseTimeMs", System.currentTimeMillis() - start);
            data.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            data.put("status", "DOWN");
            data.put("responseTimeMs", System.currentTimeMillis() - start);
            data.put("message", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> getRedisHealth() {
        Map<String, Object> data = new HashMap<>();
        long start = System.currentTimeMillis();
        try {
            Object dbSize = redisTemplate.execute((RedisCallback<Object>) RedisServerCommands::dbSize);
            data.put("status", "UP");
            data.put("responseTimeMs", System.currentTimeMillis() - start);
            data.put("keyCount", dbSize);
        } catch (Exception e) {
            data.put("status", "DOWN");
            data.put("responseTimeMs", System.currentTimeMillis() - start);
            data.put("keyCount", 0);
            data.put("message", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> getSchedulerHealth() {
        Map<String, Object> data = new HashMap<>();
        try {
            data.put("status", scheduler.isStarted() && !scheduler.isShutdown() ? "UP" : "DOWN");
            data.put("standby", scheduler.isInStandbyMode());
            data.put("jobGroupCount", scheduler.getJobGroupNames().size());
            data.put("triggerGroupCount", scheduler.getTriggerGroupNames().size());
        } catch (SchedulerException e) {
            data.put("status", "DOWN");
            data.put("standby", true);
            data.put("jobGroupCount", 0);
            data.put("triggerGroupCount", 0);
            data.put("message", e.getMessage());
        }
        return data;
    }

    private boolean allUp(Map<String, Object>... healthItems) {
        for (Map<String, Object> item : healthItems) {
            if (!"UP".equals(item.get("status"))) {
                return false;
            }
        }
        return true;
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(value * 10000.0 / total) / 100.0;
    }

    private String formatDuration(long uptimeMs) {
        Duration duration = Duration.ofMillis(uptimeMs);
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) {
            return days + "天" + hours + "小时" + minutes + "分钟";
        }
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        }
        return Math.max(minutes, 0) + "分钟";
    }

    private Set<String> getOnlineTokens(String currentToken) {
        Set<String> tokens = new LinkedHashSet<>();
        try {
            tokens.addAll(StpAdminUtil.searchTokenValue("", 0, 100, true));
        } catch (Exception e) {
            // ignore redis keys error
        }
        if (StrUtil.isNotBlank(currentToken)) {
            tokens.add(currentToken);
        }
        return tokens;
    }

    private List<Map<String, Object>> getOnlineUsers(String username, String ipaddr) {
        List<Map<String, Object>> userList = new ArrayList<>();
        String currentToken = getCurrentToken();
        for (String token : getOnlineTokens(currentToken)) {
            Object loginIdObj = getLoginId(token, token.equals(currentToken));
            Long userId = parseLong(loginIdObj);
            if (userId == null) {
                continue;
            }

            SysUser sysUser = sysUserService.getById(userId);
            if (sysUser == null || sysUserService.isInternalAccount(sysUser)) {
                continue;
            }

            Map<String, Object> user = new HashMap<>();
            user.put("sessionId", token);
            user.put("userId", userId);
            user.put("username", sysUser.getUsername());
            user.put("nickname", sysUser.getNickname());
            user.put("isCurrent", token.equals(currentToken));

            if (StrUtil.isNotEmpty(username) && !StrUtil.contains(user.get("username").toString(), username)) {
                continue;
            }
            if (StrUtil.isNotEmpty(ipaddr)) {
                continue;
            }
            userList.add(user);
        }
        return userList;
    }

    private String getCurrentToken() {
        try {
            return StpAdminUtil.getTokenValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Object getLoginId(String token, boolean isCurrent) {
        if (isCurrent) {
            try {
                Object loginId = StpAdminUtil.getLoginIdDefaultNull();
                if (loginId != null) {
                    return loginId;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        try {
            return StpAdminUtil.getLoginIdByToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
