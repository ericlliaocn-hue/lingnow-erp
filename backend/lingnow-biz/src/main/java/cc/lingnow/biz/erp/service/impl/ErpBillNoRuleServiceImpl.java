package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.entity.ErpBillNoRule;
import cc.lingnow.biz.erp.mapper.ErpBillNoRuleMapper;
import cc.lingnow.biz.erp.service.ErpBillNoRuleService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ErpBillNoRuleServiceImpl extends ServiceImpl<ErpBillNoRuleMapper, ErpBillNoRule> implements ErpBillNoRuleService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String nextNo(String billType) {
        ErpBillNoRule rule = getOne(new QueryWrapper<ErpBillNoRule>()
                .eq("bill_type", billType)
                .eq("enabled", 1)
                .last("limit 1"));
        if (rule == null) {
            return null;
        }
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern(rule.getDatePattern() == null ? "yyyyMMdd" : rule.getDatePattern()));
        long serial = rule.getNextSerial() == null || rule.getNextSerial() <= 0 ? 1L : rule.getNextSerial();
        if (shouldReset(rule, datePart)) {
            serial = 1L;
        }
        int length = rule.getSerialLength() == null || rule.getSerialLength() <= 0 ? 4 : rule.getSerialLength();
        String no = rule.getPrefix() + "-" + datePart + "-" + String.format("%0" + length + "d", serial);
        rule.setNextSerial(serial + 1);
        rule.setLastDatePart(datePart);
        updateById(rule);
        return no;
    }

    private boolean shouldReset(ErpBillNoRule rule, String datePart) {
        String resetCycle = rule.getResetCycle();
        if (StrUtil.isBlank(resetCycle) || "NONE".equals(resetCycle)) {
            return false;
        }
        String lastDatePart = rule.getLastDatePart();
        if (StrUtil.isBlank(lastDatePart)) {
            return false;
        }
        if ("MONTH".equals(resetCycle)) {
            return !monthPart(lastDatePart).equals(monthPart(datePart));
        }
        return !lastDatePart.equals(datePart);
    }

    private String monthPart(String datePart) {
        return datePart.length() <= 6 ? datePart : datePart.substring(0, 6);
    }
}
