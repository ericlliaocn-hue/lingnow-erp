-- 业务员代客下单 H5：客户订单记录来源业务员，客户账号字段兼容为空。
-- 执行前请先备份数据库。本脚本可重复执行（MySQL 8.0）。

ALTER TABLE `erp_customer_order`
    MODIFY COLUMN `account_id` bigint(20) NULL COMMENT '客户账号ID（客户自助下单时使用）';

ALTER TABLE `erp_customer_address`
    MODIFY COLUMN `account_id` bigint(20) NULL COMMENT '客户账号ID（客户自助维护时使用）';

SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'erp_customer_order' AND column_name = 'employee_id'),
    'SELECT 1',
    'ALTER TABLE `erp_customer_order` ADD COLUMN `employee_id` bigint(20) NULL COMMENT ''下单业务员ID'' AFTER `account_name`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'erp_customer_order' AND column_name = 'employee_name'),
    'SELECT 1',
    'ALTER TABLE `erp_customer_order` ADD COLUMN `employee_name` varchar(64) NULL COMMENT ''下单业务员快照'' AFTER `employee_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'erp_customer_order' AND column_name = 'source'),
    'SELECT 1',
    'ALTER TABLE `erp_customer_order` ADD COLUMN `source` varchar(32) NULL COMMENT ''订单来源 CUSTOMER_H5/SALES_H5'' AFTER `employee_name`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'erp_customer_order' AND index_name = 'idx_customer_order_employee'),
    'SELECT 1',
    'ALTER TABLE `erp_customer_order` ADD KEY `idx_customer_order_employee` (`employee_id`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
