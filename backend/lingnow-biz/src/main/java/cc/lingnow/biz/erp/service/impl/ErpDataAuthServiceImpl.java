package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.entity.ErpDataAuth;
import cc.lingnow.biz.erp.mapper.ErpDataAuthMapper;
import cc.lingnow.biz.erp.service.ErpDataAuthService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ErpDataAuthServiceImpl extends ServiceImpl<ErpDataAuthMapper, ErpDataAuth> implements ErpDataAuthService {

    @Override
    public List<Long> authorizedIds(Long userId, String resourceType) {
        if (userId == null || resourceType == null || resourceType.isBlank()) {
            return List.of();
        }
        return list(new QueryWrapper<ErpDataAuth>()
                .eq("user_id", userId)
                .eq("resource_type", resourceType)
                .eq("del_flag", 0))
                .stream()
                .map(ErpDataAuth::getResourceId)
                .toList();
    }
}
