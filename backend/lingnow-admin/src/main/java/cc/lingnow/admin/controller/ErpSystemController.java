package cc.lingnow.admin.controller;

import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.config.service.SysConfigService;
import cc.lingnow.common.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/erp/system")
@RequiredArgsConstructor
public class ErpSystemController {

    private final SysConfigService configService;

    @GetMapping("/params")
    public Result<Map<String, String>> params() {
        StpAdminUtil.stpLogic.checkPermission("erp:config:params");
        return Result.success(Map.of(
                "erp.allowNegativeStock", configValue("erp.allowNegativeStock", "N"),
                "erp.auditReadonly", configValue("erp.auditReadonly", "Y"),
                "erp.qtyPrecision", configValue("erp.qtyPrecision", "2"),
                "erp.amountPrecision", configValue("erp.amountPrecision", "2")
        ));
    }

    private String configValue(String key, String defaultValue) {
        String value = configService.selectConfigByKey(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
