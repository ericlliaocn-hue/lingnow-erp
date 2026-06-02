package cc.lingnow.biz.erp.service;

import cc.lingnow.biz.erp.entity.ErpDataAuth;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ErpDataAuthService extends IService<ErpDataAuth> {

    List<Long> authorizedIds(Long userId, String resourceType);
}
