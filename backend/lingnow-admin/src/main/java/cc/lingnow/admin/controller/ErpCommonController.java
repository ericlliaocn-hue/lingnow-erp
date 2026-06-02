package cc.lingnow.admin.controller;

import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.model.ErpAddressParseBO;
import cc.lingnow.biz.erp.model.ErpAddressParseVO;
import cc.lingnow.biz.erp.service.ErpAddressParseService;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ERP通用能力")
@RestController
@RequestMapping("/erp/common")
@RequiredArgsConstructor
public class ErpCommonController {

    private final ErpAddressParseService addressParseService;

    @PostMapping("/address/parse")
    public Result<ErpAddressParseVO> parseAddress(@Valid @RequestBody ErpAddressParseBO bo) {
        StpAdminUtil.stpLogic.checkLogin();
        return Result.success(addressParseService.parse(bo.getRawText()));
    }
}
