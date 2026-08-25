-- 销售明细价格快照与历史属性加价迁移脚本
-- 适用：MySQL 8.x
-- 注意：先备份并执行第一、二部分；第三部分只有人工确认 approval 表后才会修改历史售价。

-- ============================================================
-- 一、增加快照字段（只改表结构，不修改历史售价）
-- ============================================================
ALTER TABLE erp_bill_item
    ADD COLUMN base_price decimal(18, 4) DEFAULT NULL COMMENT '基础销售单价快照（不含属性加价）' AFTER qty,
    ADD COLUMN attribute_extra_amount decimal(18, 4) DEFAULT NULL COMMENT '单件属性加价快照' AFTER base_price,
    ADD COLUMN cost_price decimal(18, 4) DEFAULT NULL COMMENT '单件成本价快照' AFTER attribute_extra_amount;

-- 执行历史更新前保留原始明细与单据头。备份表创建后不要删除，直至业务验收完成。
CREATE TABLE IF NOT EXISTS erp_bill_item_price_backup_20260824 LIKE erp_bill_item;
INSERT IGNORE INTO erp_bill_item_price_backup_20260824
SELECT * FROM erp_bill_item;

CREATE TABLE IF NOT EXISTS erp_bill_price_backup_20260824 LIKE erp_bill;
INSERT IGNORE INTO erp_bill_price_backup_20260824
SELECT * FROM erp_bill;

-- ============================================================
-- 二、回填快照并生成待人工确认清单（不修改 price/amount/final_amount）
-- ============================================================
UPDATE erp_bill_item i
JOIN erp_bill b ON b.id = i.bill_id AND b.del_flag = 0
LEFT JOIN erp_product p ON p.id = i.product_id AND p.del_flag = 0
SET i.attribute_extra_amount = CASE
        WHEN b.bill_type LIKE 'SALE%' THEN COALESCE((
            SELECT SUM(a.extra_amount)
            FROM erp_product_attribute a
            WHERE a.del_flag = 0
              AND FIND_IN_SET(CAST(a.id AS CHAR), REPLACE(COALESCE(i.option_attribute_ids, ''), ' ', '')) > 0
        ), 0)
        ELSE 0
    END,
    i.cost_price = COALESCE(p.purchase_price, 0)
WHERE i.del_flag = 0
  AND (i.attribute_extra_amount IS NULL OR i.cost_price IS NULL);

CREATE TABLE IF NOT EXISTS erp_bill_item_price_migration_review_20260824 (
    bill_item_id bigint(20) NOT NULL,
    bill_id bigint(20) NOT NULL,
    bill_no varchar(64) DEFAULT NULL,
    bill_date date DEFAULT NULL,
    product_id bigint(20) DEFAULT NULL,
    product_code varchar(64) DEFAULT NULL,
    qty decimal(18, 4) NOT NULL,
    current_price decimal(18, 4) NOT NULL,
    current_product_sale_price decimal(18, 4) DEFAULT NULL,
    attribute_extra_amount decimal(18, 4) NOT NULL,
    expected_price decimal(18, 4) DEFAULT NULL,
    migration_status varchar(32) NOT NULL,
    PRIMARY KEY (bill_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史销售明细属性加价迁移核对表';

INSERT INTO erp_bill_item_price_migration_review_20260824 (
    bill_item_id, bill_id, bill_no, bill_date, product_id, product_code, qty,
    current_price, current_product_sale_price, attribute_extra_amount,
    expected_price, migration_status
)
SELECT
    i.id,
    i.bill_id,
    b.bill_no,
    b.bill_date,
    i.product_id,
    i.product_code,
    i.qty,
    i.price,
    p.sale_price,
    COALESCE(i.attribute_extra_amount, 0),
    COALESCE(p.sale_price, 0) + COALESCE(i.attribute_extra_amount, 0),
    CASE
        WHEN COALESCE(i.attribute_extra_amount, 0) = 0 THEN 'NO_EXTRA'
        WHEN i.price = COALESCE(p.sale_price, 0) THEN 'CANDIDATE_MISSING'
        WHEN i.price = COALESCE(p.sale_price, 0) + COALESCE(i.attribute_extra_amount, 0) THEN 'ALREADY_INCLUDED'
        ELSE 'MANUAL_REVIEW'
    END
FROM erp_bill_item i
JOIN erp_bill b ON b.id = i.bill_id AND b.del_flag = 0 AND b.bill_type = 'SALE'
LEFT JOIN erp_product p ON p.id = i.product_id AND p.del_flag = 0
WHERE i.del_flag = 0
ON DUPLICATE KEY UPDATE
    current_price = VALUES(current_price),
    current_product_sale_price = VALUES(current_product_sale_price),
    attribute_extra_amount = VALUES(attribute_extra_amount),
    expected_price = VALUES(expected_price),
    migration_status = VALUES(migration_status);

-- 只有能够按现有数据明确判断的记录才回填基础售价；歧义记录保持 NULL，等待人工核对。
UPDATE erp_bill_item i
JOIN erp_bill_item_price_migration_review_20260824 r ON r.bill_item_id = i.id
SET i.base_price = CASE
        WHEN r.migration_status = 'ALREADY_INCLUDED' THEN i.price - COALESCE(i.attribute_extra_amount, 0)
        WHEN r.migration_status IN ('CANDIDATE_MISSING', 'NO_EXTRA') THEN i.price
        ELSE NULL
    END
WHERE i.del_flag = 0
  AND i.base_price IS NULL;

-- 必须先核对这两个结果；不能仅凭 CANDIDATE_MISSING 自动更新。
SELECT migration_status, COUNT(*) AS item_count,
       SUM(qty * attribute_extra_amount) AS extra_total
FROM erp_bill_item_price_migration_review_20260824
GROUP BY migration_status
ORDER BY migration_status;

SELECT *
FROM erp_bill_item_price_migration_review_20260824
WHERE migration_status IN ('CANDIDATE_MISSING', 'MANUAL_REVIEW')
ORDER BY bill_date, bill_no, bill_item_id;

-- ============================================================
-- 三、人工批准后才执行的历史售价迁移
-- 默认 approval 表为空，因此下面事务不会修改任何销售数据。
-- 每一条必须根据原始订单/用户表格核对后手工 INSERT。
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_bill_item_price_migration_approval_20260824 (
    bill_item_id bigint(20) NOT NULL,
    approved_extra_amount decimal(18, 4) NOT NULL,
    approved_by varchar(64) NOT NULL,
    approved_remark varchar(500) DEFAULT NULL,
    PRIMARY KEY (bill_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工批准补加属性价格的历史销售明细';

-- 示例（保持注释，核对后复制并填写真实明细ID）：
-- INSERT INTO erp_bill_item_price_migration_approval_20260824
--     (bill_item_id, approved_extra_amount, approved_by, approved_remark)
-- VALUES
--     (123456789, 1.3000, '审核人', '原始订单确认该明细未包含属性加价');

START TRANSACTION;

UPDATE erp_bill_item i
JOIN erp_bill_item_price_migration_approval_20260824 a ON a.bill_item_id = i.id
JOIN erp_bill b ON b.id = i.bill_id AND b.bill_type = 'SALE' AND b.del_flag = 0
SET i.base_price = i.price,
    i.attribute_extra_amount = a.approved_extra_amount,
    i.amount = i.amount + i.qty * a.approved_extra_amount,
    i.final_amount = i.final_amount + i.qty * a.approved_extra_amount,
    i.price = i.price + a.approved_extra_amount
WHERE i.del_flag = 0;

-- 根据批准明细产生的实际差额重算受影响销售单头，保留原整单优惠、其他费用和实付金额。
UPDATE erp_bill b
JOIN (
    SELECT i.bill_id,
           SUM(i.qty) AS total_qty,
           SUM(i.amount) AS total_amount,
           SUM(i.final_amount) AS item_final_amount,
           SUM(i.discount_amount) AS item_discount_amount
    FROM erp_bill_item i
    WHERE i.del_flag = 0
      AND i.bill_id IN (
          SELECT DISTINCT bi.bill_id
          FROM erp_bill_item bi
          JOIN erp_bill_item_price_migration_approval_20260824 a ON a.bill_item_id = bi.id
      )
    GROUP BY i.bill_id
) x ON x.bill_id = b.id
SET b.total_qty = x.total_qty,
    b.total_amount = x.total_amount,
    b.payable_amount = x.item_final_amount - (b.discount_amount - x.item_discount_amount) + b.other_amount,
    b.debt_amount = (x.item_final_amount - (b.discount_amount - x.item_discount_amount) + b.other_amount) - b.paid_amount,
    b.payment_status = CASE
        WHEN b.paid_amount <= 0 THEN 'UNPAID'
        WHEN b.paid_amount >= (x.item_final_amount - (b.discount_amount - x.item_discount_amount) + b.other_amount) THEN 'PAID'
        ELSE 'PARTIAL'
    END
WHERE b.bill_type = 'SALE' AND b.del_flag = 0;

-- 提交前核对影响范围；不正确就执行 ROLLBACK，正确才改为 COMMIT。
SELECT COUNT(*) AS approved_item_count,
       COALESCE(SUM(i.qty * a.approved_extra_amount), 0) AS approved_extra_total
FROM erp_bill_item i
JOIN erp_bill_item_price_migration_approval_20260824 a ON a.bill_item_id = i.id;

ROLLBACK;
-- COMMIT;

-- ============================================================
-- 四、回滚模板（仅在已 COMMIT 且确认需要回滚时单独执行）
-- ============================================================
-- START TRANSACTION;
-- UPDATE erp_bill_item i
-- JOIN erp_bill_item_price_backup_20260824 bak ON bak.id = i.id
-- JOIN erp_bill_item_price_migration_approval_20260824 a ON a.bill_item_id = i.id
-- SET i.base_price = bak.base_price,
--     i.attribute_extra_amount = bak.attribute_extra_amount,
--     i.cost_price = bak.cost_price,
--     i.price = bak.price,
--     i.amount = bak.amount,
--     i.final_amount = bak.final_amount;
-- UPDATE erp_bill b
-- JOIN erp_bill_price_backup_20260824 bak ON bak.id = b.id
-- SET b.total_qty = bak.total_qty,
--     b.total_amount = bak.total_amount,
--     b.payable_amount = bak.payable_amount,
--     b.debt_amount = bak.debt_amount,
--     b.payment_status = bak.payment_status
-- WHERE b.id IN (
--     SELECT DISTINCT bi.bill_id
--     FROM erp_bill_item bi
--     JOIN erp_bill_item_price_migration_approval_20260824 a ON a.bill_item_id = bi.id
-- );
-- COMMIT;
