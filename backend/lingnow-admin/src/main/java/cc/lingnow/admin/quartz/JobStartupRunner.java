package cc.lingnow.admin.quartz;

import cc.lingnow.admin.manager.SysJobManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Admin 启动后注册启用状态的定时任务。
 */
@Component
@RequiredArgsConstructor
public class JobStartupRunner implements ApplicationRunner {

    private final SysJobManager jobManager;

    @Override
    public void run(ApplicationArguments args) {
        jobManager.scheduleEnabledJobs();
    }
}
