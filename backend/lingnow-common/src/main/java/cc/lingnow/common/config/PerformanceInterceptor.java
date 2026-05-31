package cc.lingnow.common.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import cc.lingnow.common.event.SlowSqlLogEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * 性能分析拦截器，用于输出 SQL 语句及执行时间
 * 同时捕获慢SQL
 *
 * @author LingNow Team
 */
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})
})
public class PerformanceInterceptor implements Interceptor, ApplicationEventPublisherAware {

    @Setter
    private long maxTime = 1000; // 慢SQL阈值，默认1000ms

    @Setter
    private boolean format = true;

    private ApplicationEventPublisher eventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 获取 SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();
        Object parameterObject = boundSql.getParameterObject();
        Configuration configuration = mappedStatement.getConfiguration();

        // 记录开始时间
        long start = System.currentTimeMillis();

        // 执行原方法
        Object result = invocation.proceed();

        // 记录结束时间
        long end = System.currentTimeMillis();
        long timing = end - start;

        // 格式化 SQL
        String formattedSql = formatSql(configuration, boundSql, sql, parameterObject);

        // 打印
        if (log.isInfoEnabled()) {
            // ANSI 颜色代码
            String RESET = "\u001B[0m";
            String RED = "\u001B[31m";
            String YELLOW = "\u001B[33m";

            log.info("\n Time：{} ms - ID：{}{}{}\n Execute SQL：{}{}{}",
                    timing,
                    YELLOW, mappedStatement.getId(), RESET,
                    RED, formattedSql, RESET);
        }

        // 慢SQL处理
        if (timing > maxTime) {
            publishSlowSqlLog(formattedSql, timing);
        }

        return result;
    }

    private void publishSlowSqlLog(String sql, long timing) {
        if (eventPublisher == null) {
            return;
        }

        try {
            Long userId = null;
            String userName = null;
            try {
                if (StpUtil.isLogin()) {
                    userId = StpUtil.getLoginIdAsLong();
                    // 这里简化处理
                }
            } catch (Exception e) {
                // ignore
            }

            SlowSqlLogEvent event = new SlowSqlLogEvent(this, sql, timing, userId, userName);
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("发布慢SQL日志事件失败", e);
        }
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
    }

    /**
     * 格式化 SQL
     */
    private String formatSql(Configuration configuration, BoundSql boundSql, String sql, Object parameterObject) {
        if (StringUtils.isBlank(sql)) {
            return "";
        }

        // 去除换行符和多余空格
        sql = sql.replaceAll("[\\s\n ]+", " ");

        try {
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            if (parameterMappings != null) {
                for (ParameterMapping parameterMapping : parameterMappings) {
                    if (parameterMapping.getMode() != ParameterMode.OUT) {
                        Object value;
                        String propertyName = parameterMapping.getProperty();
                        if (boundSql.hasAdditionalParameter(propertyName)) {
                            value = boundSql.getAdditionalParameter(propertyName);
                        } else if (parameterObject == null) {
                            value = null;
                        } else if (configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass())) {
                            value = parameterObject;
                        } else {
                            MetaObject metaObject = configuration.newMetaObject(parameterObject);
                            value = metaObject.getValue(propertyName);
                        }
                        sql = replacePlaceholder(sql, value);
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回原 SQL
            return sql;
        }
        return sql;
    }

    /**
     * 替换占位符
     */
    private String replacePlaceholder(String sql, Object propertyValue) {
        String result;
        if (propertyValue != null) {
            if (propertyValue instanceof String) {
                result = "'" + propertyValue + "'";
            } else if (propertyValue instanceof Date) {
                result = "'" + DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA)
                        .format(propertyValue) + "'";
            } else {
                result = propertyValue.toString();
            }
        } else {
            result = "null";
        }
        return sql.replaceFirst("\\?", Matcher.quoteReplacement(result));
    }
}
