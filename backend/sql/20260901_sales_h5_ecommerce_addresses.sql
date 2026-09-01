-- 销售 H5 普通电商下单：地址按业务员会话隔离，避免复用客户账号 ID。
ALTER TABLE `erp_customer_address`
    ADD COLUMN `sales_user_id` bigint(20) DEFAULT NULL COMMENT '销售H5业务员ID' AFTER `account_id`;

ALTER TABLE `erp_customer_address`
    ADD KEY `idx_customer_address_sales_user` (`sales_user_id`);
