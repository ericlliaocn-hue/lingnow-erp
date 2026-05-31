package cc.lingnow.app.config;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 *
 * @author LingNow Team
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器，打开注解式鉴权功能
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 0. 跨域预检请求直接通过
            if (cn.dev33.satoken.context.SaHolder.getRequest().getMethod().equals("OPTIONS")) {
                return;
            }

            // 1. 如果方法或类上标记了 @SaIgnore，则忽略全局校验
            if (handler instanceof HandlerMethod) {
                HandlerMethod method = (HandlerMethod) handler;
                if (method.hasMethodAnnotation(SaIgnore.class) ||
                        method.getBeanType().isAnnotationPresent(SaIgnore.class)) {
                    return;
                }
            }

            // 2. 全局认证规则
            SaRouter.match("/**")
                    .notMatch("/favicon.ico",
                            "/",
                            "/welcome",
                            "/doc.html",
                            "/webjars/**",
                            "/swagger-resources/**",
                            "/v3/api-docs/**",
                            "/auth/**",
                            "/app/auth/**",
                            "/actuator/**",
                            "/error",
                            "/files/**")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
