-- 主商品数量与每个选配项数量独立保存。
-- 执行前请备份数据库。本脚本可重复执行（MySQL 8.0）。

SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'erp_customer_order_item' AND column_name = 'option_attribute_quantity_json'),
    'SELECT 1',
    'ALTER TABLE `erp_customer_order_item` ADD COLUMN `option_attribute_quantity_json` varchar(2000) NULL COMMENT ''选配项独立数量JSON'' AFTER `option_attribute_text`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'erp_bill_item' AND column_name = 'option_attribute_quantity_json'),
    'SELECT 1',
    'ALTER TABLE `erp_bill_item` ADD COLUMN `option_attribute_quantity_json` varchar(2000) NULL COMMENT ''选配项独立数量JSON'' AFTER `option_attribute_text`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 历史记录不强行改写 JSON：后端在该字段为空时，按旧规则将每个已选属性数量解释为主商品数量，保证原单据金额口径不变。
