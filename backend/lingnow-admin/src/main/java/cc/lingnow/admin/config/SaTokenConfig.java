package cc.lingnow.admin.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.hutool.json.JSONUtil;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.common.vo.Result;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 配置
 *
 * @author LingNow Team
 */
@Configuration
public class SaTokenConfig {

    /**
     * 注册 [Sa-Token全局过滤器]
     */
    /**
     * 注册 [Sa-Token全局过滤器]
     */
    @Bean
    public FilterRegistrationBean<SaServletFilter> saServletFilter() {
        FilterRegistrationBean<SaServletFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SaServletFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico",
                        "/",
                        "/welcome",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/auth/**",
                        "/system/auth/**",
                        "/api/admin-api/**",
                        "/ws/**",
                        "/admin/file/**", // 排除认证接口和静态资源
                        "/files/**")
                .setAuth(obj -> {
                    // 跨域预检请求直接通过
                    if (SaHolder.getRequest().getMethod().equals("OPTIONS")) {
                        return;
                    }
                    // 校验登录
                    StpAdminUtil.checkLogin();
                })
                .setError(e -> {
                    // 设置响应头
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");

                    // 处理未登录异常
                    if (e instanceof cn.dev33.satoken.exception.NotLoginException) {
                        SaHolder.getResponse().setStatus(401);
                        return JSONUtil.toJsonStr(Result.error(cc.lingnow.common.enums.ErrorCode.NOT_LOGIN));
                    }

                    // 返回错误信息
                    return JSONUtil.toJsonStr(Result.error(e.getMessage()));
                })
        );
        bean.addUrlPatterns("/*");
        bean.setName("saServletFilter");
        bean.setOrder(-100); // 优先级高一点
        return bean;
    }
}
