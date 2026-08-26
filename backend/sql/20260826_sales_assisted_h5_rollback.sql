-- 仅回滚本次新增字段；不会删除已生成的订单。
-- 回滚前必须确认没有 SALES_H5 订单依赖这些字段。

ALTER TABLE `erp_customer_order` DROP INDEX `idx_customer_order_employee`;
ALTER TABLE `erp_customer_order`
    DROP COLUMN `source`,
    DROP COLUMN `employee_name`,
    DROP COLUMN `employee_id`;

-- account_id 恢复 NOT NULL 前必须先补齐所有 NULL 数据，故不在回滚脚本中自动执行。
