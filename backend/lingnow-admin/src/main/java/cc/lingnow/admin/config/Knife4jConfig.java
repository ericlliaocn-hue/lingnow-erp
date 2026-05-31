package cc.lingnow.admin.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j API文档配置
 *
 * @author LingNow Team
 */
@Configuration
@EnableKnife4j
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LingNow 管理端 API 文档")
                        .version("1.0.0")
                        .description("LingNow 匹配服务系统 - 管理端接口文档")
                        .contact(new Contact()
                                .name("LingNow Team")
                                .email("lingnow@txly.com")));
    }

}
