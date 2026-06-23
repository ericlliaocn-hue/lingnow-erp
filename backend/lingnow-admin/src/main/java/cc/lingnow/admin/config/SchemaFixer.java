package cc.lingnow.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据库Schema修复工具
 * 用于解决开发环境中数据库表结构不一致的问题
 */
@Slf4j
@Component
public class SchemaFixer implements CommandLineRunner {

    private static final String SUPERADMIN_USERNAME = "superadmin";
    private static final String SUPERADMIN_PASSWORD = "$2a$10$9oRdYxve8Vo2WRWRjO860OrdiCh.xt.uV0sQdu4tueHa0Oo6PUGGq";
    private static final String ADMIN_HIDDEN_ROOT_MENU_IDS = "1100,1200,1300,1400,1500";
    private static final String PRODUCT_ATTRIBUTE_IDS = "880000100001,880000100002,880000100003,880000100004";
    private static final String LEGACY_PRODUCT_CATEGORY_CODES = "'PRODUCT_ROOT'," +
            "'PRODUCT_STYLE'," +
            "'PRODUCT_CLOTHES_HOOK'," +
            "'PRODUCT_ACCESSORY'," +
            "'PRODUCT_CUSTOM'," +
            "'PRODUCT_STYLE_HANGER'," +
            "'PRODUCT_CLOTHES_HOOK_BULB'," +
            "'PRODUCT_CLOTHES_HOOK_ROUND'," +
            "'PRODUCT_ACCESSORY_BLACK_HOOK'," +
            "'PRODUCT_ACCESSORY_SILVER_HOOK'," +
            "'PRODUCT_CUSTOM_NO_ENGRAVE'," +
            "'PRODUCT_CUSTOM_ENGRAVE'," +
            "'PRODUCT_CUSTOM_ENGRAVE_COLOR'," +
            "'PRODUCT_CUSTOM_ENGRAVE_COLOR_RED'," +
            "'PRODUCT_CUSTOM_ENGRAVE_COLOR_BLACK'," +
            "'PRODUCT_CUSTOM_ENGRAVE_COLOR_WHITE'," +
            "'PRODUCT_CUSTOM_ENGRAVE_COLOR_SPECIAL'";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始检查并修复数据库Schema...");
        fixSysUser();
        fixGenTable();
        fixGenTableColumn();
        fixErpBill();
        fixErpProductCategory();
        seedProductCategoryTree();
        fixErpProduct();
        fixErpBillItem();
        fixErpProductMenus();
        fixAdminRoleMenus();
        log.info("数据库Schema检查修复完成");
    }

    private void fixSysUser() {
        try {
            if (!checkTableExists("sys_user")) {
                log.info("表 sys_user 不存在，跳过用户表修复");
                return;
            }
            if (!checkColumnExists("sys_user", "internal_account")) {
                log.info("表 sys_user 缺少 internal_account 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE sys_user ADD column internal_account tinyint(1) NOT NULL DEFAULT 0 COMMENT '内部开发账号 (1是 0否)' AFTER status");
            }
            ensureSuperadmin();
        } catch (Exception e) {
            log.error("修复 sys_user 失败", e);
        }
    }

    private void ensureSuperadmin() {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT user_id FROM sys_user WHERE username = ? LIMIT 1",
                Long.class,
                SUPERADMIN_USERNAME);
        if (!ids.isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE sys_user SET password = ?, nickname = ?, status = 1, internal_account = 1, del_flag = 0 WHERE user_id = ?",
                    SUPERADMIN_PASSWORD, "开发专用账号", ids.get(0));
            return;
        }
        Long userId = jdbcTemplate.queryForObject("SELECT UUID_SHORT()", Long.class);
        jdbcTemplate.update(
                "INSERT INTO sys_user (user_id, username, password, nickname, status, internal_account, create_time, del_flag) VALUES (?, ?, ?, ?, 1, 1, NOW(), 0)",
                userId, SUPERADMIN_USERNAME, SUPERADMIN_PASSWORD, "开发专用账号");
    }

    private void fixErpProductMenus() {
        try {
            if (!checkTableExists("sys_menu")) {
                log.info("表 sys_menu 不存在，跳过商品菜单修复");
                return;
            }

            jdbcTemplate.update("UPDATE sys_menu SET visible = 1 WHERE menu_id = 2010");
            jdbcTemplate.update("UPDATE sys_menu SET menu_name = '商品属性', visible = 1 WHERE menu_id = 2040");
            jdbcTemplate.update("UPDATE sys_menu SET visible = 0 WHERE menu_id IN (2020, 2030)");
        } catch (Exception e) {
            log.error("修复商品菜单显示状态失败", e);
        }
    }

    private void fixAdminRoleMenus() {
        try {
            if (!checkTableExists("sys_role_menu")) {
                log.info("表 sys_role_menu 不存在，跳过admin菜单修复");
                return;
            }
            jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = 1 AND menu_id IN (" + ADMIN_HIDDEN_ROOT_MENU_IDS + ")");
        } catch (Exception e) {
            log.error("修复admin菜单权限失败", e);
        }
    }

    private void fixErpBill() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = 'erp_bill'",
                    Integer.class);

            if (count == null || count == 0) {
                log.info("表 erp_bill 不存在，跳过修复");
                return;
            }

            if (!checkColumnExists("erp_bill", "payment_method")) {
                log.info("表 erp_bill 缺少 payment_method 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill ADD column payment_method varchar(32) DEFAULT NULL COMMENT '付款方式' AFTER paid_amount");
            }
        } catch (Exception e) {
            log.error("修复 erp_bill 失败", e);
        }
    }

    private void fixErpProductCategory() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = 'erp_product_category'",
                    Integer.class);

            if (count == null || count == 0) {
                log.info("表 erp_product_category 不存在，跳过修复");
                return;
            }

            if (!checkColumnExists("erp_product_category", "attribute_ids")) {
                log.info("表 erp_product_category 缺少 attribute_ids 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_product_category ADD column attribute_ids varchar(500) DEFAULT NULL COMMENT '关联属性节点ID集合' AFTER discount_rate");
            }
        } catch (Exception e) {
            log.error("修复 erp_product_category 失败", e);
        }
    }

    private void seedProductCategoryTree() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = 'erp_product_category'",
                    Integer.class);

            if (count == null || count == 0) {
                log.info("表 erp_product_category 不存在，跳过商品分类预置");
                return;
            }

            if (checkTableExists("erp_product_attribute")) {
                ensureProductAttribute(880000100001L, "ATTR_PRODUCT_STYLE", "商品款式", 10);
                ensureProductAttribute(880000100002L, "ATTR_CLOTHES_HOOK", "商品衣钩", 20);
                ensureProductAttribute(880000100003L, "ATTR_PRODUCT_ACCESSORY", "商品配件", 30);
                ensureProductAttribute(880000100004L, "ATTR_PRODUCT_CUSTOM", "商品定制", 40);
            }

            Long finishedHanger = ensureProductCategory("PRODUCT_FINISHED_HANGER", "成品衣架", 0L, 10, PRODUCT_ATTRIBUTE_IDS);
            Long parts = ensureProductCategory("PRODUCT_PARTS", "配件", 0L, 20, PRODUCT_ATTRIBUTE_IDS);
            migrateLegacyProductCategories(finishedHanger, parts);
            cleanupSeededProductCategoryOptions();
        } catch (Exception e) {
            log.error("预置商品分类树失败", e);
        }
    }

    private void migrateLegacyProductCategories(Long finishedHangerId, Long partsId) {
        if (!checkTableExists("erp_product")) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE erp_product SET category_id = ? " +
                        "WHERE category_id IN (SELECT id FROM erp_product_category WHERE code IN (" +
                        "'PRODUCT_ACCESSORY','PRODUCT_ACCESSORY_BLACK_HOOK','PRODUCT_ACCESSORY_SILVER_HOOK') AND del_flag = 0)",
                partsId);
        jdbcTemplate.update(
                "UPDATE erp_product SET category_id = ? " +
                        "WHERE category_id IN (SELECT id FROM erp_product_category WHERE code IN (" +
                        "'PRODUCT_ROOT','PRODUCT_STYLE','PRODUCT_CLOTHES_HOOK','PRODUCT_CUSTOM'," +
                        "'PRODUCT_STYLE_HANGER','PRODUCT_CLOTHES_HOOK_BULB','PRODUCT_CLOTHES_HOOK_ROUND'," +
                        "'PRODUCT_CUSTOM_NO_ENGRAVE','PRODUCT_CUSTOM_ENGRAVE','PRODUCT_CUSTOM_ENGRAVE_COLOR'," +
                        "'PRODUCT_CUSTOM_ENGRAVE_COLOR_RED','PRODUCT_CUSTOM_ENGRAVE_COLOR_BLACK'," +
                        "'PRODUCT_CUSTOM_ENGRAVE_COLOR_WHITE','PRODUCT_CUSTOM_ENGRAVE_COLOR_SPECIAL') AND del_flag = 0)",
                finishedHangerId);
    }

    private void cleanupSeededProductCategoryOptions() {
        jdbcTemplate.update(
                "UPDATE erp_product_category SET status = 0, del_flag = 1 " +
                        "WHERE code IN (" + LEGACY_PRODUCT_CATEGORY_CODES + ")");
    }

    private Long ensureProductCategory(String code, String name, Long parentId, int sortOrder) {
        return ensureProductCategory(code, name, parentId, sortOrder, null);
    }

    private Long ensureProductCategory(String code, String name, Long parentId, int sortOrder, String attributeIds) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM erp_product_category WHERE code = ? AND del_flag = 0 LIMIT 1",
                Long.class,
                code);
        if (!ids.isEmpty()) {
            Long id = ids.get(0);
            jdbcTemplate.update(
                    "UPDATE erp_product_category SET name = ?, parent_id = ?, attribute_ids = ?, sort_order = ?, status = 1 WHERE id = ?",
                    name, parentId, attributeIds, sortOrder, id);
            return id;
        }
        Long id = jdbcTemplate.queryForObject("SELECT UUID_SHORT()", Long.class);
        jdbcTemplate.update(
                "INSERT INTO erp_product_category (id, code, name, parent_id, attribute_ids, sort_order, status, create_time, update_time, del_flag) VALUES (?, ?, ?, ?, ?, ?, 1, NOW(), NOW(), 0)",
                id, code, name, parentId, attributeIds, sortOrder);
        return id;
    }

    private Long ensureProductAttribute(Long defaultId, String code, String name, int sortOrder) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM erp_product_attribute WHERE code = ? AND del_flag = 0 LIMIT 1",
                Long.class,
                code);
        if (!ids.isEmpty()) {
            Long id = ids.get(0);
            jdbcTemplate.update(
                    "UPDATE erp_product_attribute SET name = ?, parent_id = 0, sort_order = ?, status = 1 WHERE id = ?",
                    name, sortOrder, id);
            return id;
        }
        jdbcTemplate.update(
                "INSERT INTO erp_product_attribute (id, code, name, parent_id, sort_order, status, create_time, update_time, del_flag) VALUES (?, ?, ?, 0, ?, 1, NOW(), NOW(), 0)",
                defaultId, code, name, sortOrder);
        return defaultId;
    }

    private void fixErpProduct() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = 'erp_product'",
                    Integer.class);

            if (count == null || count == 0) {
                log.info("表 erp_product 不存在，跳过修复");
                return;
            }

            if (!checkColumnExists("erp_product", "attribute_ids")) {
                log.info("表 erp_product 缺少 attribute_ids 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_product ADD column attribute_ids varchar(500) DEFAULT NULL COMMENT '属性节点ID集合' AFTER unit_id");
            }
            if (!checkColumnExists("erp_product", "image_url")) {
                log.info("表 erp_product 缺少 image_url 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_product ADD column image_url varchar(500) DEFAULT NULL COMMENT '商品图片' AFTER max_stock");
            }
        } catch (Exception e) {
            log.error("修复 erp_product 失败", e);
        }
    }

    private void fixErpBillItem() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = 'erp_bill_item'",
                    Integer.class);

            if (count == null || count == 0) {
                log.info("表 erp_bill_item 不存在，跳过修复");
                return;
            }

            if (!checkColumnExists("erp_bill_item", "product_image_url")) {
                log.info("表 erp_bill_item 缺少 product_image_url 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column product_image_url varchar(500) DEFAULT NULL COMMENT '商品图片快照' AFTER product_name");
            }
            if (!checkColumnExists("erp_bill_item", "attribute_text")) {
                log.info("表 erp_bill_item 缺少 attribute_text 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column attribute_text varchar(500) DEFAULT NULL COMMENT '商品属性快照' AFTER spec");
            }
            if (!checkColumnExists("erp_bill_item", "category_level1_id")) {
                log.info("表 erp_bill_item 缺少 category_level1_id 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column category_level1_id bigint(20) DEFAULT NULL COMMENT '一级类目ID' AFTER attribute_text");
            }
            if (!checkColumnExists("erp_bill_item", "category_level1_name")) {
                log.info("表 erp_bill_item 缺少 category_level1_name 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column category_level1_name varchar(128) DEFAULT NULL COMMENT '一级类目快照' AFTER category_level1_id");
            }
            if (!checkColumnExists("erp_bill_item", "category_level2_id")) {
                log.info("表 erp_bill_item 缺少 category_level2_id 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column category_level2_id bigint(20) DEFAULT NULL COMMENT '二级类目ID' AFTER category_level1_name");
            }
            if (!checkColumnExists("erp_bill_item", "category_level2_name")) {
                log.info("表 erp_bill_item 缺少 category_level2_name 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column category_level2_name varchar(128) DEFAULT NULL COMMENT '二级类目快照' AFTER category_level2_id");
            }
            if (!checkColumnExists("erp_bill_item", "option_attribute_ids")) {
                log.info("表 erp_bill_item 缺少 option_attribute_ids 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column option_attribute_ids varchar(500) DEFAULT NULL COMMENT '选项属性ID集合' AFTER category_level2_name");
            }
            if (!checkColumnExists("erp_bill_item", "option_attribute_text")) {
                log.info("表 erp_bill_item 缺少 option_attribute_text 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column option_attribute_text varchar(500) DEFAULT NULL COMMENT '选项属性快照' AFTER option_attribute_ids");
            }
        } catch (Exception e) {
            log.error("修复 erp_bill_item 失败", e);
        }
    }

    private void fixGenTable() {
        try {
            // 检查gen_table是否存在
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = 'gen_table'",
                    Integer.class);

            if (count == null || count == 0) {
                log.info("表 gen_table 不存在，跳过修复");
                return;
            }

            // 检查author列
            if (!checkColumnExists("gen_table", "author")) {
                log.info("表 gen_table 缺少 author 列");
                if (checkColumnExists("gen_table", "function_author")) {
                    log.info("检测到 function_author 列，执行重命名...");
                    jdbcTemplate.execute("ALTER TABLE gen_table CHANGE function_author author varchar(50) COMMENT '生成功能作者'");
                } else {
                    log.info("创建 author 列...");
                    jdbcTemplate.execute("ALTER TABLE gen_table ADD column author varchar(50) DEFAULT NULL COMMENT '生成功能作者'");
                }
            }

            // 检查del_flag列
            if (!checkColumnExists("gen_table", "del_flag")) {
                log.info("表 gen_table 缺少 del_flag 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE gen_table ADD column del_flag tinyint(1) DEFAULT 0 COMMENT '删除标志（0代表存在 1代表删除）'");
            }

        } catch (Exception e) {
            log.error("修复 gen_table 失败", e);
        }
    }

    private void fixGenTableColumn() {
        try {
            // 检查gen_table_column是否存在
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = 'gen_table_column'",
                    Integer.class);

            if (count == null || count == 0) {
                log.info("表 gen_table_column 不存在，跳过修复");
                return;
            }

            // 检查del_flag列
            if (!checkColumnExists("gen_table_column", "del_flag")) {
                log.info("表 gen_table_column 缺少 del_flag 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE gen_table_column ADD column del_flag tinyint(1) DEFAULT 0 COMMENT '删除标志（0代表存在 1代表删除）'");
            }

        } catch (Exception e) {
            log.error("修复 gen_table_column 失败", e);
        }
    }

    private boolean checkTableExists(String tableName) {
        String sql = "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean checkColumnExists(String tableName, String columnName) {
        String sql = "SELECT count(*) FROM information_schema.columns WHERE table_schema = (SELECT DATABASE()) AND table_name = ? AND column_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
