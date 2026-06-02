package cc.lingnow.biz.erp.service;

import cc.lingnow.biz.erp.entity.ErpBillNoRule;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ErpBillNoRuleService extends IService<ErpBillNoRule> {
    String nextNo(String billType);
}
