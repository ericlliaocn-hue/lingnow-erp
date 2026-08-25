-- 18080 对比库专用：销售单样品标记与 2026 年 7 月蒙兰兰历史核定成本
-- 业务口径：样品计入销售额，但不计成本和利润；是否样品只按明确订单标记，不按价格自动判断。
-- 安全边界：脚本只允许在 lingnow_erp_compare_20260824 执行，拒绝正式库。

DELIMITER //
DROP PROCEDURE IF EXISTS assert_compare_database//
CREATE PROCEDURE assert_compare_database()
BEGIN
    IF DATABASE() <> 'lingnow_erp_compare_20260824' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '拒绝执行：本脚本仅允许用于 lingnow_erp_compare_20260824';
    END IF;
END//
CALL assert_compare_database()//
DROP PROCEDURE assert_compare_database//
DELIMITER ;

SET @add_sample_column = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'erp_bill' AND column_name = 'sample'
    ),
    'SELECT 1',
    'ALTER TABLE erp_bill ADD COLUMN sample tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否样品 0否 1是；样品计入销售额但不计利润'' AFTER paid_amount'
);
PREPARE add_sample_column_stmt FROM @add_sample_column;
EXECUTE add_sample_column_stmt;
DEALLOCATE PREPARE add_sample_column_stmt;

-- 兼容已执行过旧版对比迁移的数据库：只复制旧标记，不再让业务代码读取旧字段。
SET @copy_legacy_exclude_flag = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'erp_bill' AND column_name = 'exclude_from_profit'
    ),
    'UPDATE erp_bill SET sample = exclude_from_profit WHERE exclude_from_profit = 1',
    'SELECT 1'
);
PREPARE copy_legacy_exclude_flag_stmt FROM @copy_legacy_exclude_flag;
EXECUTE copy_legacy_exclude_flag_stmt;
DEALLOCATE PREPARE copy_legacy_exclude_flag_stmt;

SET @add_profit_cost_column = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'erp_bill' AND column_name = 'profit_cost_amount'
    ),
    'SELECT 1',
    'ALTER TABLE erp_bill ADD COLUMN profit_cost_amount decimal(18,4) DEFAULT NULL COMMENT ''利润核算成本（历史核定整单成本）'' AFTER sample'
);
PREPARE add_profit_cost_column_stmt FROM @add_profit_cost_column;
EXECUTE add_profit_cost_column_stmt;
DEALLOCATE PREPARE add_profit_cost_column_stmt;

CREATE TABLE IF NOT EXISTS erp_bill_sample_rule_backup_20260825 LIKE erp_bill;
INSERT IGNORE INTO erp_bill_sample_rule_backup_20260825 SELECT * FROM erp_bill;

CREATE TABLE IF NOT EXISTS erp_bill_sample_rule_migration_20260825 (
    bill_no varchar(64) NOT NULL,
    sample tinyint(1) NOT NULL DEFAULT 0,
    profit_cost_amount decimal(18,4) DEFAULT NULL,
    source_remark varchar(500) NOT NULL,
    PRIMARY KEY (bill_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='18080 对比库样品与利润口径人工核对清单';

INSERT INTO erp_bill_sample_rule_migration_20260825
    (bill_no, sample, profit_cost_amount, source_remark)
VALUES
    -- 用户确认的样品订单，共9单、实付合计219.30元。价格只是描述，不作为自动判断条件。
    ('XS-20260703-0014', 1, NULL, '用户7月核对表：罗静喆19.90，样品'),
    ('XS-20260703-0016', 1, NULL, '用户7月核对表：倩倩19.90，样品'),
    ('XS-20260705-0011', 1, NULL, '用户7月核对表：陈园29.90，样品'),
    ('XS-20260713-0012', 1, NULL, '用户7月核对表：莹莹19.90，样品'),
    ('XS-20260718-0003', 1, NULL, '用户后续确认：倩倩20.00，样品'),
    ('XS-20260720-0006', 1, NULL, '用户后续确认：刘诗达30.00，样品'),
    ('XS-20260725-0008', 1, NULL, '用户7月核对表：屈子伟29.90，样品'),
    ('XS-20260726-0005', 1, NULL, '用户7月核对表：grace19.90，样品'),
    ('XS-20260728-0004', 1, NULL, '用户7月核对表：曹静29.90，样品'),

    -- 正常订单历史核定整单成本：合计24,270.30元。
    ('XS-20260704-0004', 0, 136.00, '用户7月核对表：董甜'),
    ('XS-20260706-0021', 0, 330.00, '用户7月核对表：关关'),
    ('XS-20260713-0009', 0, 2160.00, '用户7月核对表：侯燕'),
    ('XS-20260713-0014', 0, 2940.00, '用户7月核对表：何小姐'),
    ('XS-20260714-0011', 0, 900.00, '用户7月核对表：哈哈'),
    ('XS-20260714-0012', 0, 2493.00, '用户7月核对表：杜宇/希逗商贸'),
    ('XS-20260715-0009', 0, 1671.50, '用户7月核对表：祝丽萍'),
    ('XS-20260716-0006', 0, 1180.00, '用户7月核对表：巫利霞'),
    ('XS-20260718-0002', 0, 580.00, '用户7月核对表：徐晓翠'),
    ('XS-20260720-0008', 0, 1400.00, '用户7月核对表：木木'),
    ('XS-20260722-0006', 0, 1400.00, '用户7月核对表：蔡欣怡'),
    ('XS-20260727-0008', 0, 80.00, '用户7月核对表：关关'),
    ('XS-20260728-0003', 0, 3150.00, '用户确认王森实付3570，历史核定成本3150'),
    ('XS-20260801-0004', 0, 659.80, '用户7月核对表：grace'),
    ('XS-20260801-0005', 0, 1782.00, '用户7月核对表：李欣'),
    ('XS-20260801-0006', 0, 1218.00, '用户7月核对表：孙箫箫'),
    ('XS-20260806-0001', 0, 2190.00, '用户7月核对表：罗静喆2，实付已补录2574')
ON DUPLICATE KEY UPDATE
    sample = VALUES(sample),
    profit_cost_amount = VALUES(profit_cost_amount),
    source_remark = VALUES(source_remark);

DELIMITER //
DROP PROCEDURE IF EXISTS apply_compare_sample_rule//
CREATE PROCEDURE apply_compare_sample_rule()
BEGIN
    DECLARE matched_count int DEFAULT 0;
    DECLARE matched_paid decimal(18,4) DEFAULT 0;
    DECLARE sample_paid decimal(18,4) DEFAULT 0;
    DECLARE mapped_cost decimal(18,4) DEFAULT 0;

    SELECT COUNT(*),
           COALESCE(SUM(b.paid_amount), 0),
           COALESCE(SUM(CASE WHEN m.sample = 1 THEN b.paid_amount ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN m.sample = 0 THEN m.profit_cost_amount ELSE 0 END), 0)
    INTO matched_count, matched_paid, sample_paid, mapped_cost
    FROM erp_bill_sample_rule_migration_20260825 m
    JOIN erp_bill b ON b.bill_no = m.bill_no
    WHERE b.del_flag = 0
      AND b.bill_type = 'SALE'
      AND b.employee_name = '蒙兰兰'
      AND b.bill_date >= '2026-07-01'
      AND b.bill_date < '2026-08-01';

    IF matched_count <> 26 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移终止：未准确匹配蒙兰兰7月的26张销售单';
    END IF;
    IF matched_paid <> 30424.3000 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移终止：26张销售单实付合计不是30424.30';
    END IF;
    IF sample_paid <> 219.3000 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移终止：样品订单实付合计不是219.30';
    END IF;
    IF mapped_cost <> 24270.3000 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移终止：用户核定成本合计不是24270.30';
    END IF;

    START TRANSACTION;

    UPDATE erp_bill b
    JOIN erp_bill_sample_rule_migration_20260825 m ON m.bill_no = b.bill_no
    SET b.sample = m.sample,
        b.profit_cost_amount = m.profit_cost_amount
    WHERE b.del_flag = 0
      AND b.bill_type = 'SALE'
      AND b.employee_name = '蒙兰兰'
      AND b.bill_date >= '2026-07-01'
      AND b.bill_date < '2026-08-01';

    COMMIT;
END//
CALL apply_compare_sample_rule()//
DROP PROCEDURE apply_compare_sample_rule//
DELIMITER ;

-- 验收结果应为：26单、销售额30424.30、样品9单/219.30、核定成本24270.30、利润5934.70。
SELECT
    COUNT(*) AS bill_count,
    SUM(b.paid_amount) AS sale_amount,
    SUM(CASE WHEN b.sample = 1 THEN 1 ELSE 0 END) AS sample_bill_count,
    SUM(CASE WHEN b.sample = 1 THEN b.paid_amount ELSE 0 END) AS sample_paid_amount,
    SUM(CASE WHEN b.sample = 0 THEN COALESCE(b.profit_cost_amount, 0) ELSE 0 END) AS profit_cost_amount,
    SUM(CASE WHEN b.sample = 0 THEN b.paid_amount - COALESCE(b.profit_cost_amount, 0) ELSE 0 END) AS profit_amount
FROM erp_bill b
WHERE b.del_flag = 0
  AND b.bill_type = 'SALE'
  AND b.employee_name = '蒙兰兰'
  AND b.bill_date >= '2026-07-01'
  AND b.bill_date < '2026-08-01';
