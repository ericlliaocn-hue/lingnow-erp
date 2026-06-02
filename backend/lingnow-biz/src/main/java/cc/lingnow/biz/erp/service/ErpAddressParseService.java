package cc.lingnow.biz.erp.service;

import cc.lingnow.biz.erp.model.ErpAddressParseVO;

public interface ErpAddressParseService {

    ErpAddressParseVO parse(String rawText);
}
