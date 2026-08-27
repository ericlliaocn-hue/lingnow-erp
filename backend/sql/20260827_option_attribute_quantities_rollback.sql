-- 仅回滚字段结构；执行前请确认新订单中的独立选配数量已另行备份。
ALTER TABLE `erp_customer_order_item` DROP COLUMN `option_attribute_quantity_json`;
ALTER TABLE `erp_bill_item` DROP COLUMN `option_attribute_quantity_json`;
