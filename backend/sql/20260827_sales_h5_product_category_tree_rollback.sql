-- 回滚 20260827 业务员 H5 商品分类迁移。
-- 仅恢复首次执行迁移脚本前已经存在的分类状态和商品 category_id。

UPDATE `erp_product` p
JOIN `erp_product_category_mapping_backup_20260827` b ON b.`product_id` = p.`id`
SET p.`category_id` = b.`category_id`, p.`update_time` = NOW();

UPDATE `erp_product_category` c
JOIN `erp_product_category_state_backup_20260827` b ON b.`id` = c.`id`
SET c.`code` = b.`code`,
    c.`name` = b.`name`,
    c.`parent_id` = b.`parent_id`,
    c.`attribute_ids` = b.`attribute_ids`,
    c.`sort_order` = b.`sort_order`,
    c.`status` = b.`status`,
    c.`del_flag` = b.`del_flag`,
    c.`update_time` = NOW();

-- 首次迁移后新增的 SALES_H5_* 分类不物理删除，停用并软删除，便于审计。
UPDATE `erp_product_category` c
LEFT JOIN `erp_product_category_state_backup_20260827` b ON b.`id` = c.`id`
SET c.`status` = 0, c.`del_flag` = 1, c.`update_time` = NOW()
WHERE c.`code` LIKE 'SALES_H5_%' AND b.`id` IS NULL;

SELECT COUNT(*) AS `restored_product_mappings`
FROM `erp_product_category_mapping_backup_20260827`;
