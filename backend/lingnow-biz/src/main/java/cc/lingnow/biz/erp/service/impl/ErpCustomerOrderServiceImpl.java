package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.entity.ErpCustomerOrder;
import cc.lingnow.biz.erp.mapper.ErpCustomerOrderMapper;
import cc.lingnow.biz.erp.service.ErpCustomerOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ErpCustomerOrderServiceImpl extends ServiceImpl<ErpCustomerOrderMapper, ErpCustomerOrder> implements ErpCustomerOrderService {
}
