package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.entity.ErpAccount;
import cc.lingnow.biz.erp.mapper.ErpAccountMapper;
import cc.lingnow.biz.erp.service.ErpAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ErpAccountServiceImpl extends ServiceImpl<ErpAccountMapper, ErpAccount> implements ErpAccountService {
}
