-- 商品规格关联从属性组细化到具体选项。
ALTER TABLE `erp_product`
    ADD COLUMN `option_attribute_ids` varchar(2000) DEFAULT NULL COMMENT '商品可选项ID集合' AFTER `attribute_ids`;

ALTER TABLE `erp_product`
    ADD KEY `idx_product_option_attribute` (`option_attribute_ids`(191));
