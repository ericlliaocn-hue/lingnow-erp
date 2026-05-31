package cc.lingnow.admin.quartz;

/**
 * 定时任务常量
 */
public final class JobConstants {

    private JobConstants() {
    }

    public static final Integer STATUS_NORMAL = 1;
    public static final Integer STATUS_PAUSED = 0;

    public static final String CONCURRENT_ALLOWED = "Y";
    public static final String CONCURRENT_DISALLOWED = "N";

    public static final String MISFIRE_DEFAULT = "DEFAULT";
    public static final String MISFIRE_IGNORE = "IGNORE";
    public static final String MISFIRE_FIRE_ONCE = "FIRE_ONCE";
    public static final String MISFIRE_DO_NOTHING = "DO_NOTHING";

    public static final String JOB_DATA_KEY = "SYS_JOB";
}
