package cc.lingnow.biz.erp.service;

import cc.lingnow.biz.erp.model.ErpAddressRegionVO;

import java.util.List;

public interface ErpAddressRegionService {

    List<ErpAddressRegionVO> listChildren(String parentCode);

    List<ErpAddressRegionVO> search(String keyword, int limit);

    ErpAddressRegionVO matchAddress(String text);
}
