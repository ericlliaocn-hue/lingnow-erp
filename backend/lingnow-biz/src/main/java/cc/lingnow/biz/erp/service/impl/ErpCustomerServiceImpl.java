package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.entity.ErpCustomer;
import cc.lingnow.biz.erp.mapper.ErpCustomerMapper;
import cc.lingnow.biz.erp.service.ErpCustomerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ErpCustomerServiceImpl extends ServiceImpl<ErpCustomerMapper, ErpCustomer> implements ErpCustomerService {
}
