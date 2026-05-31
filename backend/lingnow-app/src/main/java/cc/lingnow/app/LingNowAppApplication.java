package cc.lingnow.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * LingNow App 应用启动类
 *
 * @author LingNow Team
 */
@EnableCaching
@SpringBootApplication(scanBasePackages = "cc.lingnow")
public class LingNowAppApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(LingNowAppApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port", "6061");
        System.out.println("\n========================================");
        System.out.println("LingNow App 用户端应用启动成功！");
        System.out.println("API文档地址: http://localhost:" + port + "/doc.html");
        System.out.println("========================================\n");
    }

}
