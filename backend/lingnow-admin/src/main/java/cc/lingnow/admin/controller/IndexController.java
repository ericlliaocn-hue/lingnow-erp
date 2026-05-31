package cc.lingnow.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器
 *
 * @author LingNow Team
 */
@RestController
public class IndexController {

    @GetMapping("/")
    public String index() {
        return "Welcome to LingNow ERP API! Service is running.";
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to LingNow ERP API! Service is running.";
    }

    @GetMapping("/favicon.ico")
    public void favicon() {
        // 返回空，避免 404 报错
    }
}
