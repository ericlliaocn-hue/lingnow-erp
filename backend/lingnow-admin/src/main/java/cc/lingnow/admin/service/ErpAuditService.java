package cc.lingnow.admin.service;

public interface ErpAuditService {

    void auditBill(Long id);

    void unauditBill(Long id);

    void auditFinanceBill(Long id);

    void unauditFinanceBill(Long id);

    void auditStockCheck(Long id);

    void unauditStockCheck(Long id);
}
