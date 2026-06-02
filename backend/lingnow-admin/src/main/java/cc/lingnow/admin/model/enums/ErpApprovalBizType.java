package cc.lingnow.admin.model.enums;

import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;

public enum ErpApprovalBizType {
    SALE("销售单", "sale", "erp:sale"),
    SALE_RETURN("销售退货单", "sale-return", "erp:sale-return"),
    PURCHASE("进货单", "purchase", "erp:purchase"),
    PURCHASE_RETURN("进货退货单", "purchase-return", "erp:purchase-return"),
    STOCK_CHECK("库存盘点", "check", "erp:stock-check"),
    RECEIPT("收款单", "receipt", "erp:finance:receipt"),
    PAYMENT("付款单", "payment", "erp:finance:payment"),
    INCOME("其他收入", "income", "erp:finance:income"),
    EXPENSE("其他支出", "expense", "erp:finance:expense");

    private final String label;
    private final String module;
    private final String permissionPrefix;

    ErpApprovalBizType(String label, String module, String permissionPrefix) {
        this.label = label;
        this.module = module;
        this.permissionPrefix = permissionPrefix;
    }

    public String label() {
        return label;
    }

    public String module() {
        return module;
    }

    public String permissionPrefix() {
        return permissionPrefix;
    }

    public static ErpApprovalBizType of(String bizType) {
        try {
            return ErpApprovalBizType.valueOf(bizType);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "业务类型不支持");
        }
    }
}
