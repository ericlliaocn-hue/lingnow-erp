package cc.lingnow.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * LingNow Admin 应用启动类
 *
 * @author LingNow Team
 */
@EnableCaching
@SpringBootApplication(scanBasePackages = "cc.lingnow")
public class LingNowAdminApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(LingNowAdminApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port", "6060");
        System.out.println("\n========================================");
        System.out.println("LingNow Admin 管理端应用启动成功！");
        System.out.println("API文档地址: http://localhost:" + port + "/doc.html");
        System.out.println("========================================\n");
    }

}
