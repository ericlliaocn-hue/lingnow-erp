package cc.lingnow.admin.quartz;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Bean 获取工具，用于定时任务白名单 Bean 调用。
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static Object getBean(String beanName) {
        if (applicationContext == null || !applicationContext.containsBean(beanName)) {
            return null;
        }
        return applicationContext.getBean(beanName);
    }
}
