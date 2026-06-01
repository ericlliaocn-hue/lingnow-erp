package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.entity.ErpProduct;
import cc.lingnow.biz.erp.mapper.ErpProductMapper;
import cc.lingnow.biz.erp.service.ErpProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ErpProductServiceImpl extends ServiceImpl<ErpProductMapper, ErpProduct> implements ErpProductService {
}
