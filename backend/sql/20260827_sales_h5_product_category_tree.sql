-- 业务员代客下单 H5：建立真实三级商品分类，并按当前商品名称迁移分类。
-- 仅在确认过的数据库执行；执行前会在同库保存原分类和商品映射，可用配套 rollback 脚本恢复。
-- MySQL 8.0，可重复执行。

CREATE TABLE IF NOT EXISTS `erp_product_category_state_backup_20260827` (
    `id` bigint(20) NOT NULL,
    `code` varchar(64) NOT NULL,
    `name` varchar(128) NOT NULL,
    `parent_id` bigint(20) DEFAULT '0',
    `attribute_ids` varchar(500) DEFAULT NULL,
    `sort_order` int(11) DEFAULT '0',
    `status` tinyint(1) NOT NULL DEFAULT '1',
    `del_flag` tinyint(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `erp_product_category_state_backup_20260827`
(`id`, `code`, `name`, `parent_id`, `attribute_ids`, `sort_order`, `status`, `del_flag`)
SELECT `id`, `code`, `name`, `parent_id`, `attribute_ids`, `sort_order`, `status`, `del_flag`
FROM `erp_product_category`;

CREATE TABLE IF NOT EXISTS `erp_product_category_mapping_backup_20260827` (
    `product_id` bigint(20) NOT NULL,
    `category_id` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `erp_product_category_mapping_backup_20260827` (`product_id`, `category_id`)
SELECT `id`, `category_id` FROM `erp_product`;

DROP TEMPORARY TABLE IF EXISTS `tmp_sales_h5_category_definition`;
CREATE TEMPORARY TABLE `tmp_sales_h5_category_definition` (
    `code` varchar(64) NOT NULL,
    `name` varchar(128) NOT NULL,
    `parent_code` varchar(64) DEFAULT NULL,
    `attribute_ids` varchar(500) DEFAULT NULL,
    `sort_order` int NOT NULL,
    `level_no` int NOT NULL,
    PRIMARY KEY (`code`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_sales_h5_category_definition`
(`code`, `name`, `parent_code`, `attribute_ids`, `sort_order`, `level_no`) VALUES
('SALES_H5_HANGER', '衣架（含裤架）', NULL, '880000100001,880000100002,880000100003,880000100004', 10, 1),
('SALES_H5_HOOK', '衣钩', NULL, NULL, 20, 1),
('SALES_H5_ACCESSORY', '配件', NULL, NULL, 30, 1),

('SALES_H5_HANGER_CHILD', '童装', 'SALES_H5_HANGER', NULL, 10, 2),
('SALES_H5_HANGER_LOTUS', '荷木', 'SALES_H5_HANGER', NULL, 20, 2),
('SALES_H5_HANGER_RUBBER', '橡胶木', 'SALES_H5_HANGER', NULL, 30, 2),
('SALES_H5_HANGER_BEECH', '榉木', 'SALES_H5_HANGER', NULL, 40, 2),
('SALES_H5_HANGER_RESIN', '树脂', 'SALES_H5_HANGER', NULL, 50, 2),
('SALES_H5_HANGER_UNKNOWN', '材质待确认', 'SALES_H5_HANGER', NULL, 60, 2),
('SALES_H5_HOOK_S', 'S钩', 'SALES_H5_HOOK', NULL, 10, 2),
('SALES_H5_HOOK_U_CLIP', 'U型夹', 'SALES_H5_HOOK', NULL, 20, 2),
('SALES_H5_HOOK_RING', '圈圈', 'SALES_H5_HOOK', NULL, 30, 2),
('SALES_H5_ACCESSORY_ANTI_SLIP', '防滑贴', 'SALES_H5_ACCESSORY', NULL, 10, 2),
('SALES_H5_ACCESSORY_COVER', '布套', 'SALES_H5_ACCESSORY', NULL, 20, 2),

('SALES_H5_HANGER_CHILD_HANGER', '童装衣架', 'SALES_H5_HANGER_CHILD', NULL, 10, 3),
('SALES_H5_HANGER_CHILD_PANTS', '裤架', 'SALES_H5_HANGER_CHILD', NULL, 20, 3),
('SALES_H5_HANGER_LOTUS_FEMALE', '女款衣架', 'SALES_H5_HANGER_LOTUS', NULL, 10, 3),
('SALES_H5_HANGER_LOTUS_MALE', '男款衣架', 'SALES_H5_HANGER_LOTUS', NULL, 20, 3),
('SALES_H5_HANGER_LOTUS_PANTS', '裤架', 'SALES_H5_HANGER_LOTUS', NULL, 30, 3),
('SALES_H5_HANGER_RUBBER_FEMALE', '女款衣架', 'SALES_H5_HANGER_RUBBER', NULL, 10, 3),
('SALES_H5_HANGER_RUBBER_MALE', '男款衣架', 'SALES_H5_HANGER_RUBBER', NULL, 20, 3),
('SALES_H5_HANGER_RUBBER_PANTS', '裤架', 'SALES_H5_HANGER_RUBBER', NULL, 30, 3),
('SALES_H5_HANGER_BEECH_FEMALE', '女款衣架', 'SALES_H5_HANGER_BEECH', NULL, 10, 3),
('SALES_H5_HANGER_BEECH_PANTS', '裤架', 'SALES_H5_HANGER_BEECH', NULL, 20, 3),
('SALES_H5_HANGER_RESIN_HANGER', '通用衣架', 'SALES_H5_HANGER_RESIN', NULL, 10, 3),
('SALES_H5_HANGER_RESIN_PANTS', '裤架', 'SALES_H5_HANGER_RESIN', NULL, 20, 3),
('SALES_H5_HANGER_UNKNOWN_FEMALE', '女款衣架', 'SALES_H5_HANGER_UNKNOWN', NULL, 10, 3),
('SALES_H5_HANGER_UNKNOWN_MALE', '男款衣架', 'SALES_H5_HANGER_UNKNOWN', NULL, 20, 3),
('SALES_H5_HANGER_UNKNOWN_PANTS', '裤架', 'SALES_H5_HANGER_UNKNOWN', NULL, 30, 3),
('SALES_H5_HOOK_S_5CM', '5CM', 'SALES_H5_HOOK_S', NULL, 10, 3),
('SALES_H5_HOOK_S_10CM', '10CM', 'SALES_H5_HOOK_S', NULL, 20, 3),
('SALES_H5_HOOK_S_15CM', '15CM', 'SALES_H5_HOOK_S', NULL, 30, 3),
('SALES_H5_HOOK_U_CLIP_WHITE', '奶白色', 'SALES_H5_HOOK_U_CLIP', NULL, 10, 3),
('SALES_H5_HOOK_U_CLIP_BLACK', '黑色', 'SALES_H5_HOOK_U_CLIP', NULL, 20, 3),
('SALES_H5_HOOK_U_CLIP_WALNUT', '胡桃木色', 'SALES_H5_HOOK_U_CLIP', NULL, 30, 3),
('SALES_H5_HOOK_RING_WHITE', '奶白色', 'SALES_H5_HOOK_RING', NULL, 10, 3),
('SALES_H5_HOOK_RING_BLACK', '黑色', 'SALES_H5_HOOK_RING', NULL, 20, 3),
('SALES_H5_HOOK_RING_WALNUT', '胡桃木色', 'SALES_H5_HOOK_RING', NULL, 30, 3),
('SALES_H5_ACCESSORY_ANTI_SLIP_WHITE', '白色', 'SALES_H5_ACCESSORY_ANTI_SLIP', NULL, 10, 3),
('SALES_H5_ACCESSORY_ANTI_SLIP_BROWN', '棕色', 'SALES_H5_ACCESSORY_ANTI_SLIP', NULL, 20, 3),
('SALES_H5_ACCESSORY_ANTI_SLIP_CLEAR', '透明', 'SALES_H5_ACCESSORY_ANTI_SLIP', NULL, 30, 3),
('SALES_H5_ACCESSORY_COVER_WHITE_HANGER', '白色衣架布套', 'SALES_H5_ACCESSORY_COVER', NULL, 10, 3),
('SALES_H5_ACCESSORY_COVER_WHITE_PANTS', '白色裤架布套', 'SALES_H5_ACCESSORY_COVER', NULL, 20, 3),
('SALES_H5_ACCESSORY_COVER_BEIGE_HANGER', '米白色衣架布套', 'SALES_H5_ACCESSORY_COVER', NULL, 30, 3),
('SALES_H5_ACCESSORY_COVER_BEIGE_PANTS', '米白色裤架布套', 'SALES_H5_ACCESSORY_COVER', NULL, 40, 3);

-- 分三级插入，确保父节点先存在。
INSERT INTO `erp_product_category`
(`id`, `code`, `name`, `parent_id`, `attribute_ids`, `sort_order`, `status`, `create_time`, `update_time`, `del_flag`)
SELECT UUID_SHORT(), d.`code`, d.`name`, 0, d.`attribute_ids`, d.`sort_order`, 1, NOW(), NOW(), 0
FROM `tmp_sales_h5_category_definition` d
WHERE d.`level_no` = 1
  AND NOT EXISTS (SELECT 1 FROM `erp_product_category` c WHERE c.`code` = d.`code` AND c.`del_flag` = 0);

INSERT INTO `erp_product_category`
(`id`, `code`, `name`, `parent_id`, `attribute_ids`, `sort_order`, `status`, `create_time`, `update_time`, `del_flag`)
SELECT UUID_SHORT(), d.`code`, d.`name`, p.`id`, d.`attribute_ids`, d.`sort_order`, 1, NOW(), NOW(), 0
FROM `tmp_sales_h5_category_definition` d
JOIN `erp_product_category` p ON p.`code` = d.`parent_code` AND p.`del_flag` = 0
WHERE d.`level_no` = 2
  AND NOT EXISTS (SELECT 1 FROM `erp_product_category` c WHERE c.`code` = d.`code` AND c.`del_flag` = 0);

INSERT INTO `erp_product_category`
(`id`, `code`, `name`, `parent_id`, `attribute_ids`, `sort_order`, `status`, `create_time`, `update_time`, `del_flag`)
SELECT UUID_SHORT(), d.`code`, d.`name`, p.`id`, d.`attribute_ids`, d.`sort_order`, 1, NOW(), NOW(), 0
FROM `tmp_sales_h5_category_definition` d
JOIN `erp_product_category` p ON p.`code` = d.`parent_code` AND p.`del_flag` = 0
WHERE d.`level_no` = 3
  AND NOT EXISTS (SELECT 1 FROM `erp_product_category` c WHERE c.`code` = d.`code` AND c.`del_flag` = 0);

UPDATE `erp_product_category` c
JOIN `tmp_sales_h5_category_definition` d ON d.`code` = c.`code`
LEFT JOIN `erp_product_category` p ON p.`code` = d.`parent_code` AND p.`del_flag` = 0
SET c.`name` = d.`name`,
    c.`parent_id` = IFNULL(p.`id`, 0),
    c.`attribute_ids` = d.`attribute_ids`,
    c.`sort_order` = d.`sort_order`,
    c.`status` = 1,
    c.`del_flag` = 0,
    c.`update_time` = NOW()
WHERE c.`del_flag` = 0;

-- 商品名称映射规则：先识别衣钩/配件，再按材质和款式划分衣架。
UPDATE `erp_product` p
JOIN `erp_product_category` c ON c.`code` = CASE
    WHEN (p.`name` LIKE '%S钩%' OR p.`name` LIKE '%s钩%') AND (p.`name` LIKE '%15CM%' OR p.`name` LIKE '%15cm%' OR p.`name` LIKE '%15ＣＭ%') THEN 'SALES_H5_HOOK_S_15CM'
    WHEN (p.`name` LIKE '%S钩%' OR p.`name` LIKE '%s钩%') AND (p.`name` LIKE '%10CM%' OR p.`name` LIKE '%10cm%' OR p.`name` LIKE '%10ＣＭ%') THEN 'SALES_H5_HOOK_S_10CM'
    WHEN p.`name` LIKE '%S钩%' OR p.`name` LIKE '%s钩%' THEN 'SALES_H5_HOOK_S_5CM'
    WHEN (p.`name` LIKE '%U型夹%' OR p.`name` LIKE '%u型夹%') AND p.`name` LIKE '%黑%' THEN 'SALES_H5_HOOK_U_CLIP_BLACK'
    WHEN (p.`name` LIKE '%U型夹%' OR p.`name` LIKE '%u型夹%') AND p.`name` LIKE '%胡桃%' THEN 'SALES_H5_HOOK_U_CLIP_WALNUT'
    WHEN p.`name` LIKE '%U型夹%' OR p.`name` LIKE '%u型夹%' THEN 'SALES_H5_HOOK_U_CLIP_WHITE'
    WHEN p.`name` LIKE '%圈圈%' AND p.`name` LIKE '%黑%' THEN 'SALES_H5_HOOK_RING_BLACK'
    WHEN p.`name` LIKE '%圈圈%' AND p.`name` LIKE '%胡桃%' THEN 'SALES_H5_HOOK_RING_WALNUT'
    WHEN p.`name` LIKE '%圈圈%' THEN 'SALES_H5_HOOK_RING_WHITE'
    WHEN p.`name` LIKE '%防滑贴%' AND p.`name` LIKE '%棕%' THEN 'SALES_H5_ACCESSORY_ANTI_SLIP_BROWN'
    WHEN p.`name` LIKE '%防滑贴%' AND (p.`name` LIKE '%透明%' OR p.`name` LIKE '%清%') THEN 'SALES_H5_ACCESSORY_ANTI_SLIP_CLEAR'
    WHEN p.`name` LIKE '%防滑贴%' THEN 'SALES_H5_ACCESSORY_ANTI_SLIP_WHITE'
    WHEN p.`name` LIKE '%布套%' AND p.`name` LIKE '%裤%' AND (p.`name` LIKE '%米白%' OR p.`name` LIKE '%奶白%') THEN 'SALES_H5_ACCESSORY_COVER_BEIGE_PANTS'
    WHEN p.`name` LIKE '%布套%' AND (p.`name` LIKE '%米白%' OR p.`name` LIKE '%奶白%') THEN 'SALES_H5_ACCESSORY_COVER_BEIGE_HANGER'
    WHEN p.`name` LIKE '%布套%' AND p.`name` LIKE '%裤%' THEN 'SALES_H5_ACCESSORY_COVER_WHITE_PANTS'
    WHEN p.`name` LIKE '%布套%' THEN 'SALES_H5_ACCESSORY_COVER_WHITE_HANGER'
    WHEN p.`name` LIKE '%童装%' AND p.`name` LIKE '%裤架%' THEN 'SALES_H5_HANGER_CHILD_PANTS'
    WHEN p.`name` LIKE '%童装%' THEN 'SALES_H5_HANGER_CHILD_HANGER'
    WHEN p.`name` LIKE '%荷木%' AND p.`name` LIKE '%裤架%' THEN 'SALES_H5_HANGER_LOTUS_PANTS'
    WHEN p.`name` LIKE '%荷木%' AND p.`name` LIKE '%男%' THEN 'SALES_H5_HANGER_LOTUS_MALE'
    WHEN p.`name` LIKE '%荷木%' THEN 'SALES_H5_HANGER_LOTUS_FEMALE'
    WHEN p.`name` LIKE '%橡胶木%' AND p.`name` LIKE '%裤架%' THEN 'SALES_H5_HANGER_RUBBER_PANTS'
    WHEN p.`name` LIKE '%橡胶木%' AND p.`name` LIKE '%男%' THEN 'SALES_H5_HANGER_RUBBER_MALE'
    WHEN p.`name` LIKE '%橡胶木%' THEN 'SALES_H5_HANGER_RUBBER_FEMALE'
    WHEN p.`name` LIKE '%榉木%' AND p.`name` LIKE '%裤架%' THEN 'SALES_H5_HANGER_BEECH_PANTS'
    WHEN p.`name` LIKE '%榉木%' THEN 'SALES_H5_HANGER_BEECH_FEMALE'
    WHEN p.`name` LIKE '%树脂%' AND p.`name` LIKE '%裤架%' THEN 'SALES_H5_HANGER_RESIN_PANTS'
    WHEN p.`name` LIKE '%树脂%' THEN 'SALES_H5_HANGER_RESIN_HANGER'
    WHEN p.`name` LIKE '%裤架%' THEN 'SALES_H5_HANGER_UNKNOWN_PANTS'
    WHEN p.`name` LIKE '%男%' THEN 'SALES_H5_HANGER_UNKNOWN_MALE'
    ELSE 'SALES_H5_HANGER_UNKNOWN_FEMALE'
END AND c.`del_flag` = 0
SET p.`category_id` = c.`id`, p.`update_time` = NOW()
WHERE p.`del_flag` = 0 AND p.`status` = 1;

-- 旧的扁平分类不再展示；备份表保留了原状态。
UPDATE `erp_product_category`
SET `status` = 0, `del_flag` = 1, `update_time` = NOW()
WHERE `del_flag` = 0
  AND `code` IN (
    'PRODUCT_FINISHED_HANGER', 'PRODUCT_PARTS', 'PRODUCT_ROOT', 'PRODUCT_STYLE',
    'PRODUCT_CLOTHES_HOOK', 'PRODUCT_ACCESSORY', 'PRODUCT_CUSTOM', 'PRODUCT_STYLE_HANGER',
    'PRODUCT_CLOTHES_HOOK_BULB', 'PRODUCT_CLOTHES_HOOK_ROUND',
    'PRODUCT_ACCESSORY_BLACK_HOOK', 'PRODUCT_ACCESSORY_SILVER_HOOK',
    'PRODUCT_CUSTOM_NO_ENGRAVE', 'PRODUCT_CUSTOM_ENGRAVE', 'PRODUCT_CUSTOM_ENGRAVE_COLOR',
    'PRODUCT_CUSTOM_ENGRAVE_COLOR_RED', 'PRODUCT_CUSTOM_ENGRAVE_COLOR_BLACK',
    'PRODUCT_CUSTOM_ENGRAVE_COLOR_WHITE', 'PRODUCT_CUSTOM_ENGRAVE_COLOR_SPECIAL'
  );

-- 验证：应无启用商品缺少分类，并展示各三级分类数量。
SELECT COUNT(*) AS `active_product_without_category`
FROM `erp_product` p
LEFT JOIN `erp_product_category` c ON c.`id` = p.`category_id` AND c.`del_flag` = 0 AND c.`status` = 1
WHERE p.`del_flag` = 0 AND p.`status` = 1 AND c.`id` IS NULL;

SELECT c.`code`, c.`name`, COUNT(p.`id`) AS `product_count`
FROM `erp_product_category` c
LEFT JOIN `erp_product` p ON p.`category_id` = c.`id` AND p.`del_flag` = 0 AND p.`status` = 1
WHERE c.`code` LIKE 'SALES_H5_%' AND c.`del_flag` = 0
GROUP BY c.`id`, c.`code`, c.`name`
ORDER BY c.`code`;
