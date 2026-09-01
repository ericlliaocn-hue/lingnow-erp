package cc.lingnow.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据库Schema修复工具
 * 用于解决开发环境中数据库表结构不一致的问题
 */
@Slf4j
@Component
public class SchemaFixer implements CommandLineRunner {

    private static final String SUPERADMIN_USERNAME = "superadmin";
    private static final String SUPERADMIN_PASSWORD = "$2a$10$9oRdYxve8Vo2WRWRjO860OrdiCh.xt.uV0sQdu4tueHa0Oo6PUGGq";
    private static final String ADMIN_HIDDEN_ROOT_MENU_IDS = "1200,1300,1400,1500";
    private static final String ADMIN_SYSTEM_MENU_IDS = "1100,1110,1111,1112,1113,1114,1115,1120,1121,1122,1123,1124,1125";
    private static final long PRODUCT_COST_EDIT_MENU_ID = 2057L;
    private static final String PRODUCTION_MENU_IDS = "2250,2251,2252,2253";
    private static final String CUSTOMER_ORDER_MENU_IDS = "2260,2261,2262,2263";
    private static final String CUSTOMER_ACCOUNT_MENU_IDS = "2190,2191,2192";
    private static final String SALESPERSON_PRODUCTION_MENU_IDS = "2250,2251,2252";
    private static final String SALESPERSON_CUSTOMER_ORDER_MENU_IDS = "2260,2263";
    private static final long SALESPERSON_ROLE_ID = 880000200001L;
    private static final String SALESPERSON_ROLE_KEY = "salesperson";
    private static final String COST_PRICE_EDIT_PERMISSION = "erp:product:cost:edit";
    private static final String SALESPERSON_MENU_IDS = "1000," +
            "2000,2010,2020,2030,2040,2050,2052,2054,2056," +
            "2100,2110,2111,2112,2130,2131,2140," +
            "2200,2210,2220,2211,2212,2213,2216,2217," +
            "2230,2240,2231,2232,2233,2236,2237," +
            SALESPERSON_PRODUCTION_MENU_IDS + "," + SALESPERSON_CUSTOMER_ORDER_MENU_IDS;
    private static final String PRODUCT_ATTRIBUTE_IDS = "880000100001,880000100002,880000100003,880000100004";
    private static final String LEGACY_PRODUCT_CATEGORY_CODES = "'PRODUCT_FINISHED_HANGER'," +
            "'PRODUCT_PARTS'," +
            "'PRODUCT_ROOT'," +
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

    @Value("${lingnow.schema-fixer.enabled:true}")
    private boolean enabled;

    @Override
    public void run(String... args) throws Exception {
        if (!enabled) {
            log.info("数据库Schema修复已关闭，跳过自动修复");
            return;
        }
        log.info("开始检查并修复数据库Schema...");
        fixSysUser();
        fixSysRoleDept();
        fixGenTable();
        fixGenTableColumn();
        fixErpBill();
        fixErpProductCategory();
        fixErpProductAttribute();
        seedProductCategoryTree();
        fixErpProduct();
        fixErpBillItem();
        fixCustomerOrderTables();
        fixErpProductMenus();
        fixProductionMenus();
        fixCustomerOrderMenus();
        fixEmployeeReportMenus();
        fixErpApprovalMenus();
        fixProductCostEditPermission();
        fixAdminRoleMenus();
        fixSalespersonRole();
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

            jdbcTemplate.update("UPDATE sys_menu SET visible = 0 WHERE menu_id = 2010");
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
            if (checkTableExists("sys_menu")) {
                jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) " +
                        "SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (" + ADMIN_SYSTEM_MENU_IDS + ")");
            }
        } catch (Exception e) {
            log.error("修复admin菜单权限失败", e);
        }
    }

    private void fixProductCostEditPermission() {
        try {
            if (!checkTableExists("sys_menu")) {
                log.info("表 sys_menu 不存在，跳过成本价权限菜单修复");
                return;
            }
            ensureProductCostEditMenu();
        } catch (Exception e) {
            log.error("修复成本价权限菜单失败", e);
        }
    }

    private void fixSalespersonRole() {
        try {
            if (!checkTableExists("sys_role") || !checkTableExists("sys_role_menu") || !checkTableExists("sys_menu")) {
                log.info("角色或菜单表不存在，跳过业务员角色修复");
                return;
            }
            Long roleId = ensureSalespersonRole();
            jdbcTemplate.update("DELETE rm FROM sys_role_menu rm " +
                            "INNER JOIN sys_menu m ON rm.menu_id = m.menu_id " +
                            "WHERE rm.role_id = ? AND m.permission = ?",
                    roleId, COST_PRICE_EDIT_PERMISSION);
            jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) " +
                            "SELECT ?, menu_id FROM sys_menu WHERE menu_id IN (" + SALESPERSON_MENU_IDS + ")",
                    roleId);
            jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ? AND menu_id = 2253", roleId);
        } catch (Exception e) {
            log.error("修复业务员角色失败", e);
        }
    }

    private void fixSysRoleDept() {
        try {
            if (!checkTableExists("sys_role_dept")) {
                log.info("表 sys_role_dept 不存在，创建中...");
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_role_dept (" +
                        "role_id bigint(20) NOT NULL COMMENT '角色ID'," +
                        "dept_id bigint(20) NOT NULL COMMENT '部门ID'," +
                        "PRIMARY KEY (role_id, dept_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和部门关联'");
            }
        } catch (Exception e) {
            log.error("修复 sys_role_dept 失败", e);
        }
    }

    private Long ensureProductCostEditMenu() {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT menu_id FROM sys_menu WHERE permission = ? LIMIT 1",
                Long.class,
                COST_PRICE_EDIT_PERMISSION);
        Long menuId = ids.isEmpty() ? availableId(PRODUCT_COST_EDIT_MENU_ID, "sys_menu", "menu_id") : ids.get(0);
        if (ids.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, icon, path, component, permission, sort_order, visible, status, is_cache, create_time, del_flag) " +
                            "VALUES (?, 2050, '成本价编辑', 2, NULL, NULL, NULL, ?, 7, 1, 1, 'N', NOW(), 0)",
                    menuId, COST_PRICE_EDIT_PERMISSION);
        } else {
            jdbcTemplate.update(
                    "UPDATE sys_menu SET parent_id = 2050, menu_name = '成本价编辑', menu_type = 2, permission = ?, status = 1, del_flag = 0 WHERE menu_id = ?",
                    COST_PRICE_EDIT_PERMISSION, menuId);
        }
        return menuId;
    }

    private Long ensureSalespersonRole() {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT role_id FROM sys_role WHERE role_key = ? LIMIT 1",
                Long.class,
                SALESPERSON_ROLE_KEY);
        if (!ids.isEmpty()) {
            Long roleId = ids.get(0);
            jdbcTemplate.update(
                    "UPDATE sys_role SET role_name = '业务员', role_key = ?, sort_order = 2, status = 1, data_scope = 1, del_flag = 0 WHERE role_id = ?",
                    SALESPERSON_ROLE_KEY, roleId);
            return roleId;
        }
        Long roleId = availableId(SALESPERSON_ROLE_ID, "sys_role", "role_id");
        jdbcTemplate.update(
                "INSERT INTO sys_role (role_id, role_name, role_key, sort_order, status, data_scope, remark, create_time, del_flag) " +
                        "VALUES (?, '业务员', ?, 2, 1, 1, '销售业务员角色，可查看成本价但不能修改成本价', NOW(), 0)",
                roleId, SALESPERSON_ROLE_KEY);
        return roleId;
    }

    private void fixErpApprovalMenus() {
        try {
            if (!checkTableExists("sys_menu")) {
                log.info("表 sys_menu 不存在，跳过审批菜单修复");
                return;
            }
            jdbcTemplate.update("UPDATE sys_menu SET visible = 0 WHERE menu_id BETWEEN 1500 AND 1540");
        } catch (Exception e) {
            log.error("修复审批菜单显示状态失败", e);
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
            if (!checkColumnExists("erp_bill", "employee_name")) {
                log.info("表 erp_bill 缺少 employee_name 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill ADD column employee_name varchar(64) DEFAULT NULL COMMENT '业务员' AFTER employee_id");
            }
            if (!checkColumnExists("erp_bill", "production_progress")) {
                log.info("表 erp_bill 缺少 production_progress 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill ADD column production_progress varchar(64) DEFAULT NULL COMMENT '生产进度' AFTER payment_status");
            }
            if (!checkColumnExists("erp_bill", "production_user_id")) {
                log.info("表 erp_bill 缺少 production_user_id 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill ADD column production_user_id bigint(20) DEFAULT NULL COMMENT '生产人员ID' AFTER production_progress");
            }
            if (!checkColumnExists("erp_bill", "production_user_name")) {
                log.info("表 erp_bill 缺少 production_user_name 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill ADD column production_user_name varchar(64) DEFAULT NULL COMMENT '生产人员' AFTER production_user_id");
            }
            if (!checkColumnExists("erp_bill", "tracking_no")) {
                log.info("表 erp_bill 缺少 tracking_no 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill ADD column tracking_no varchar(100) DEFAULT NULL COMMENT '快递单号' AFTER production_progress");
            }
        } catch (Exception e) {
            log.error("修复 erp_bill 失败", e);
        }
    }

    private void fixProductionMenus() {
        try {
            if (!checkTableExists("sys_menu")) {
                log.info("表 sys_menu 不存在，跳过生产单菜单修复");
                return;
            }
            ensureMenu(2250L, 2200L, "生产单", 1, "Document", "/erp/production/list", "erp/bill/index", "erp:production:list", 125);
            ensureMenu(2251L, 2250L, "生产单复制", 2, null, null, null, "erp:production:copy", 1);
            ensureMenu(2252L, 2250L, "生产单打印", 2, null, null, null, "erp:production:print", 2);
            ensureMenu(2253L, 2250L, "生产单维护", 2, null, null, null, "erp:production:edit", 3);
            if (checkTableExists("sys_role_menu")) {
                jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) " +
                        "SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (" + PRODUCTION_MENU_IDS + ")");
            }
        } catch (Exception e) {
            log.error("修复生产单菜单失败", e);
        }
    }

    private void fixCustomerOrderMenus() {
        try {
            if (!checkTableExists("sys_menu")) {
                log.info("表 sys_menu 不存在，跳过客户订单菜单修复");
                return;
            }
            ensureMenu(2190L, 2100L, "客户账号", 1, "User", "/erp/setting/customer-account", "erp/customerAccount/index", "erp:customer-account:list", 120);
            ensureMenu(2191L, 2190L, "客户账号新增", 2, null, null, null, "erp:customer-account:add", 1);
            ensureMenu(2192L, 2190L, "客户账号编辑", 2, null, null, null, "erp:customer-account:edit", 2);
            ensureMenu(2260L, 2200L, "客户订单", 1, "Tickets", "/erp/customer-order/list", "erp/customerOrder/index", "erp:customer-order:list", 126);
            ensureMenu(2261L, 2260L, "客户订单确认", 2, null, null, null, "erp:customer-order:confirm", 1);
            ensureMenu(2262L, 2260L, "客户订单作废", 2, null, null, null, "erp:customer-order:cancel", 2);
            ensureMenu(2263L, 2260L, "客户订单打印", 2, null, null, null, "erp:customer-order:print", 3);
            if (checkTableExists("sys_role_menu")) {
                jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) " +
                        "SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (" + CUSTOMER_ORDER_MENU_IDS + "," + CUSTOMER_ACCOUNT_MENU_IDS + ")");
                jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) " +
                        "SELECT ?, menu_id FROM sys_menu WHERE menu_id IN (" + SALESPERSON_CUSTOMER_ORDER_MENU_IDS + ")",
                        ensureSalespersonRole());
            }
        } catch (Exception e) {
            log.error("修复客户订单菜单失败", e);
        }
    }

    private void fixEmployeeReportMenus() {
        try {
            if (!checkTableExists("sys_menu")) {
                log.info("表 sys_menu 不存在，跳过业务员报表菜单修复");
                return;
            }
            jdbcTemplate.update("UPDATE sys_menu SET menu_name = '业务员业绩统计' WHERE menu_id = 2740 OR path = '/erp/report/employee-performance'");
            jdbcTemplate.update("UPDATE sys_menu SET menu_name = '业务员业绩提成' WHERE menu_id = 2750 OR path = '/erp/report/employee-commission'");
        } catch (Exception e) {
            log.error("修复业务员报表菜单失败", e);
        }
    }

    private Long ensureMenu(Long preferredId, Long parentId, String menuName, Integer menuType, String icon, String path,
                            String component, String permission, Integer sortOrder) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT menu_id FROM sys_menu WHERE permission = ? LIMIT 1",
                Long.class,
                permission);
        Long menuId = ids.isEmpty() ? availableId(preferredId, "sys_menu", "menu_id") : ids.get(0);
        if (ids.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, icon, path, component, permission, sort_order, visible, status, is_cache, create_time, del_flag) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 'N', NOW(), 0)",
                    menuId, parentId, menuName, menuType, icon, path, component, permission, sortOrder);
        } else {
            jdbcTemplate.update(
                    "UPDATE sys_menu SET parent_id = ?, menu_name = ?, menu_type = ?, icon = ?, path = ?, component = ?, permission = ?, sort_order = ?, visible = 1, status = 1, is_cache = 'N', del_flag = 0 WHERE menu_id = ?",
                    parentId, menuName, menuType, icon, path, component, permission, sortOrder, menuId);
        }
        return menuId;
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

    private void fixCustomerOrderTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS erp_customer_account (" +
                    "id bigint(20) NOT NULL COMMENT '客户账号ID'," +
                    "customer_id bigint(20) NOT NULL COMMENT '客户ID'," +
                    "username varchar(64) NOT NULL COMMENT '登录账号'," +
                    "password varchar(128) NOT NULL COMMENT '登录密码'," +
                    "nickname varchar(64) DEFAULT NULL COMMENT '昵称'," +
                    "phone varchar(32) DEFAULT NULL COMMENT '手机号'," +
                    "status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 (1启用 0禁用)'," +
                    "last_login_time datetime DEFAULT NULL COMMENT '最后登录时间'," +
                    "remark varchar(500) DEFAULT NULL COMMENT '备注'," +
                    "create_time datetime DEFAULT NULL," +
                    "update_time datetime DEFAULT NULL," +
                    "create_by varchar(64) DEFAULT NULL," +
                    "update_by varchar(64) DEFAULT NULL," +
                    "del_flag tinyint(1) NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (id)," +
                    "UNIQUE KEY uk_customer_account_username (username, del_flag)," +
                    "KEY idx_customer_account_customer (customer_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP客户登录账号'");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS erp_customer_address (" +
                    "id bigint(20) NOT NULL COMMENT '客户地址ID'," +
                    "customer_id bigint(20) NOT NULL COMMENT '客户ID'," +
                    "account_id bigint(20) NOT NULL COMMENT '客户账号ID'," +
                    "sales_user_id bigint(20) DEFAULT NULL COMMENT '销售H5业务员ID'," +
                    "receiver_name varchar(64) NOT NULL COMMENT '收货人'," +
                    "receiver_phone varchar(32) NOT NULL COMMENT '收货电话'," +
                    "province_code varchar(32) DEFAULT NULL COMMENT '省编码'," +
                    "province_name varchar(64) DEFAULT NULL COMMENT '省名称'," +
                    "city_code varchar(32) DEFAULT NULL COMMENT '市编码'," +
                    "city_name varchar(64) DEFAULT NULL COMMENT '市名称'," +
                    "district_code varchar(32) DEFAULT NULL COMMENT '区县编码'," +
                    "district_name varchar(64) DEFAULT NULL COMMENT '区县名称'," +
                    "street_code varchar(32) DEFAULT NULL COMMENT '镇街编码'," +
                    "street_name varchar(128) DEFAULT NULL COMMENT '镇街名称'," +
                    "village_code varchar(32) DEFAULT NULL COMMENT '村社区编码'," +
                    "village_name varchar(128) DEFAULT NULL COMMENT '村社区名称'," +
                    "detail_address varchar(500) NOT NULL COMMENT '详细地址'," +
                    "full_address varchar(800) DEFAULT NULL COMMENT '完整地址'," +
                    "address_label varchar(32) DEFAULT NULL COMMENT '地址标签'," +
                    "default_flag tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认'," +
                    "create_time datetime DEFAULT NULL," +
                    "update_time datetime DEFAULT NULL," +
                    "create_by varchar(64) DEFAULT NULL," +
                    "update_by varchar(64) DEFAULT NULL," +
                    "del_flag tinyint(1) NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (id)," +
                    "KEY idx_customer_address_customer (customer_id)," +
                    "KEY idx_customer_address_account (account_id)," +
                    "KEY idx_customer_address_sales_user (sales_user_id)," +
                    "KEY idx_customer_address_default (account_id, default_flag)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP客户H5收货地址'");
            if (!checkColumnExists("erp_customer_address", "sales_user_id")) {
                log.info("表 erp_customer_address 缺少 sales_user_id 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_customer_address ADD column sales_user_id bigint(20) DEFAULT NULL COMMENT '销售H5业务员ID' AFTER account_id");
                jdbcTemplate.execute("ALTER TABLE erp_customer_address ADD KEY idx_customer_address_sales_user (sales_user_id)");
            }

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS erp_customer_order (" +
                    "id bigint(20) NOT NULL COMMENT '客户订单ID'," +
                    "order_no varchar(64) NOT NULL COMMENT '客户订单号'," +
                    "customer_id bigint(20) NOT NULL COMMENT '客户ID'," +
                    "customer_name varchar(128) DEFAULT NULL COMMENT '客户名称快照'," +
                    "account_id bigint(20) NOT NULL COMMENT '客户账号ID'," +
                    "account_name varchar(64) DEFAULT NULL COMMENT '下单账号快照'," +
                    "status varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/CONFIRMED/CANCELLED'," +
                    "order_time datetime NOT NULL COMMENT '下单时间'," +
                    "total_qty decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '总数量'," +
                    "total_amount decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '总金额'," +
                    "receiver_name varchar(64) DEFAULT NULL COMMENT '收货人'," +
                    "receiver_phone varchar(32) DEFAULT NULL COMMENT '收货电话'," +
                    "receiver_address varchar(500) DEFAULT NULL COMMENT '收货地址'," +
                    "remark varchar(500) DEFAULT NULL COMMENT '备注'," +
                    "bill_id bigint(20) DEFAULT NULL COMMENT '销售单ID'," +
                    "bill_no varchar(64) DEFAULT NULL COMMENT '销售单号'," +
                    "confirm_time datetime DEFAULT NULL COMMENT '确认时间'," +
                    "confirm_by varchar(64) DEFAULT NULL COMMENT '确认人'," +
                    "cancel_time datetime DEFAULT NULL COMMENT '作废时间'," +
                    "cancel_by varchar(64) DEFAULT NULL COMMENT '作废人'," +
                    "cancel_reason varchar(500) DEFAULT NULL COMMENT '作废原因'," +
                    "create_time datetime DEFAULT NULL," +
                    "update_time datetime DEFAULT NULL," +
                    "create_by varchar(64) DEFAULT NULL," +
                    "update_by varchar(64) DEFAULT NULL," +
                    "del_flag tinyint(1) NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (id)," +
                    "UNIQUE KEY uk_customer_order_no (order_no, del_flag)," +
                    "KEY idx_customer_order_customer (customer_id)," +
                    "KEY idx_customer_order_account (account_id)," +
                    "KEY idx_customer_order_status (status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP客户H5订单'");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS erp_customer_order_item (" +
                    "id bigint(20) NOT NULL COMMENT '客户订单明细ID'," +
                    "order_id bigint(20) NOT NULL COMMENT '客户订单ID'," +
                    "product_id bigint(20) NOT NULL COMMENT '商品ID'," +
                    "product_code varchar(64) DEFAULT NULL COMMENT '商品编号快照'," +
                    "product_name varchar(128) DEFAULT NULL COMMENT '商品名称快照'," +
                    "product_image_url varchar(500) DEFAULT NULL COMMENT '商品图片快照'," +
                    "logo_image_url varchar(500) DEFAULT NULL COMMENT 'LOGO图片'," +
                    "spec varchar(128) DEFAULT NULL COMMENT '规格快照'," +
                    "attribute_text varchar(500) DEFAULT NULL COMMENT '商品属性快照'," +
                    "category_level1_id bigint(20) DEFAULT NULL COMMENT '一级类目ID'," +
                    "category_level1_name varchar(128) DEFAULT NULL COMMENT '一级类目快照'," +
                    "category_level2_id bigint(20) DEFAULT NULL COMMENT '二级类目ID'," +
                    "category_level2_name varchar(128) DEFAULT NULL COMMENT '二级类目快照'," +
                    "option_attribute_ids varchar(500) DEFAULT NULL COMMENT '选项属性ID集合'," +
                    "option_attribute_text varchar(500) DEFAULT NULL COMMENT '选项属性快照'," +
                    "option_attribute_quantity_json varchar(2000) DEFAULT NULL COMMENT '选配项独立数量JSON'," +
                    "unit_id bigint(20) DEFAULT NULL COMMENT '单位ID'," +
                    "qty decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '数量'," +
                    "price decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '单价'," +
                    "amount decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '金额'," +
                    "remark varchar(500) DEFAULT NULL COMMENT '定制说明'," +
                    "create_time datetime DEFAULT NULL," +
                    "update_time datetime DEFAULT NULL," +
                    "create_by varchar(64) DEFAULT NULL," +
                    "update_by varchar(64) DEFAULT NULL," +
                    "del_flag tinyint(1) NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (id)," +
                    "KEY idx_customer_order_item_order (order_id)," +
                    "KEY idx_customer_order_item_product (product_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP客户H5订单明细'");
            if (!checkColumnExists("erp_customer_order_item", "option_attribute_quantity_json")) {
                log.info("表 erp_customer_order_item 缺少 option_attribute_quantity_json 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_customer_order_item ADD column option_attribute_quantity_json varchar(2000) DEFAULT NULL COMMENT '选配项独立数量JSON' AFTER option_attribute_text");
            }
        } catch (Exception e) {
            log.error("修复客户订单表失败", e);
        }
    }

    private void fixErpProductAttribute() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = 'erp_product_attribute'",
                    Integer.class);

            if (count == null || count == 0) {
                log.info("表 erp_product_attribute 不存在，跳过修复");
                return;
            }

            if (!checkColumnExists("erp_product_attribute", "extra_amount")) {
                log.info("表 erp_product_attribute 缺少 extra_amount 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_product_attribute ADD column extra_amount decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '配置加价金额' AFTER discount_rate");
            }
        } catch (Exception e) {
            log.error("修复 erp_product_attribute 失败", e);
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
                Long style = ensureProductAttribute(880000100001L, "ATTR_PRODUCT_STYLE", "商品款式", 10);
                Long hook = ensureProductAttribute(880000100002L, "ATTR_CLOTHES_HOOK", "商品衣钩", 20);
                Long accessory = ensureProductAttribute(880000100003L, "ATTR_PRODUCT_ACCESSORY", "商品配件", 30);
                Long custom = ensureProductAttribute(880000100004L, "ATTR_PRODUCT_CUSTOM", "商品定制", 40);
                migrateLegacyProductAttributes(style, hook, accessory, custom);
            }

            seedSalesH5CategoryTree();
            cleanupSeededProductCategoryOptions();
            cleanupOrphanedProductCategoryRoots();
        } catch (Exception e) {
            log.error("预置商品分类树失败", e);
        }
    }

    private void seedSalesH5CategoryTree() {
        Long hanger = ensureProductCategory("SALES_H5_HANGER", "衣架（含裤架）", 0L, 10, PRODUCT_ATTRIBUTE_IDS);
        seedHangerMaterial(hanger, "CHILD", "童装", 10, "HANGER", "童装衣架", "PANTS", "裤架");
        seedHangerMaterial(hanger, "LOTUS", "荷木", 20, "FEMALE", "女款衣架", "MALE", "男款衣架", "PANTS", "裤架");
        seedHangerMaterial(hanger, "RUBBER", "橡胶木", 30, "FEMALE", "女款衣架", "MALE", "男款衣架", "PANTS", "裤架");
        seedHangerMaterial(hanger, "BEECH", "榉木", 40, "FEMALE", "女款衣架", "PANTS", "裤架");
        seedHangerMaterial(hanger, "RESIN", "树脂", 50, "HANGER", "通用衣架", "PANTS", "裤架");
        seedHangerMaterial(hanger, "UNKNOWN", "材质待确认", 60, "FEMALE", "女款衣架", "MALE", "男款衣架", "PANTS", "裤架");

        Long hook = ensureProductCategory("SALES_H5_HOOK", "衣钩", 0L, 20);
        Long sHook = ensureProductCategory("SALES_H5_HOOK_S", "S钩", hook, 10);
        ensureProductCategory("SALES_H5_HOOK_S_5CM", "5CM", sHook, 10);
        ensureProductCategory("SALES_H5_HOOK_S_10CM", "10CM", sHook, 20);
        ensureProductCategory("SALES_H5_HOOK_S_15CM", "15CM", sHook, 30);
        Long uClip = ensureProductCategory("SALES_H5_HOOK_U_CLIP", "U型夹", hook, 20);
        seedHookColors(uClip, "U_CLIP");
        Long ring = ensureProductCategory("SALES_H5_HOOK_RING", "圈圈", hook, 30);
        seedHookColors(ring, "RING");

        Long accessory = ensureProductCategory("SALES_H5_ACCESSORY", "配件", 0L, 30);
        Long antiSlip = ensureProductCategory("SALES_H5_ACCESSORY_ANTI_SLIP", "防滑贴", accessory, 10);
        ensureProductCategory("SALES_H5_ACCESSORY_ANTI_SLIP_WHITE", "白色", antiSlip, 10);
        ensureProductCategory("SALES_H5_ACCESSORY_ANTI_SLIP_BROWN", "棕色", antiSlip, 20);
        ensureProductCategory("SALES_H5_ACCESSORY_ANTI_SLIP_CLEAR", "透明", antiSlip, 30);
        Long cover = ensureProductCategory("SALES_H5_ACCESSORY_COVER", "布套", accessory, 20);
        ensureProductCategory("SALES_H5_ACCESSORY_COVER_WHITE_HANGER", "白色衣架布套", cover, 10);
        ensureProductCategory("SALES_H5_ACCESSORY_COVER_WHITE_PANTS", "白色裤架布套", cover, 20);
        ensureProductCategory("SALES_H5_ACCESSORY_COVER_BEIGE_HANGER", "米白色衣架布套", cover, 30);
        ensureProductCategory("SALES_H5_ACCESSORY_COVER_BEIGE_PANTS", "米白色裤架布套", cover, 40);
    }

    private void seedHangerMaterial(Long rootId, String codePart, String name, int sortOrder,
                                    String... leafCodesAndNames) {
        Long material = ensureProductCategory("SALES_H5_HANGER_" + codePart, name, rootId, sortOrder);
        for (int index = 0; index + 1 < leafCodesAndNames.length; index += 2) {
            ensureProductCategory("SALES_H5_HANGER_" + codePart + "_" + leafCodesAndNames[index],
                    leafCodesAndNames[index + 1], material, (index / 2 + 1) * 10);
        }
    }

    private void seedHookColors(Long parentId, String codePart) {
        ensureProductCategory("SALES_H5_HOOK_" + codePart + "_WHITE", "奶白色", parentId, 10);
        ensureProductCategory("SALES_H5_HOOK_" + codePart + "_BLACK", "黑色", parentId, 20);
        ensureProductCategory("SALES_H5_HOOK_" + codePart + "_WALNUT", "胡桃木色", parentId, 30);
    }

    private void cleanupSeededProductCategoryOptions() {
        jdbcTemplate.update(
                "UPDATE erp_product_category SET status = 0, del_flag = 1 " +
                        "WHERE code IN (" + LEGACY_PRODUCT_CATEGORY_CODES + ")");
    }

    private void cleanupOrphanedProductCategoryRoots() {
        if (!checkTableExists("erp_product")) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE erp_product_category c SET c.status = 0, c.del_flag = 1, c.update_time = NOW() " +
                        "WHERE c.parent_id = 0 AND ((c.code = '配件' AND c.name = '衣钩') " +
                        "OR (c.code = '01' AND c.name = '10CM银色圆钩')) " +
                        "AND NOT EXISTS (SELECT 1 FROM erp_product p WHERE p.category_id = c.id AND p.del_flag = 0)");
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

    private void migrateLegacyProductAttributes(Long styleId, Long hookId, Long accessoryId, Long customId) {
        List<Long> standardIds = List.of(styleId, hookId, accessoryId, customId);
        List<Map<String, Object>> legacyRoots = jdbcTemplate.queryForList(
                "SELECT id, name FROM erp_product_attribute WHERE del_flag = 0 AND parent_id = 0 AND id NOT IN (?, ?, ?, ?)",
                styleId, hookId, accessoryId, customId);
        for (Map<String, Object> row : legacyRoots) {
            Long id = ((Number) row.get("id")).longValue();
            String name = String.valueOf(row.get("name"));
            jdbcTemplate.update("UPDATE erp_product_attribute SET parent_id = ? WHERE id = ?",
                    inferAttributeGroup(name, styleId, hookId, accessoryId, customId), id);
        }

        for (int i = 0; i < 4; i++) {
            List<Map<String, Object>> nestedOptions = jdbcTemplate.queryForList(
                    "SELECT id, name FROM erp_product_attribute WHERE del_flag = 0 AND parent_id NOT IN (0, ?, ?, ?, ?)",
                    styleId, hookId, accessoryId, customId);
            if (nestedOptions.isEmpty()) {
                return;
            }
            for (Map<String, Object> row : nestedOptions) {
                Long id = ((Number) row.get("id")).longValue();
                if (standardIds.contains(id)) {
                    continue;
                }
                String name = String.valueOf(row.get("name"));
                jdbcTemplate.update("UPDATE erp_product_attribute SET parent_id = ? WHERE id = ?",
                        inferAttributeGroup(name, styleId, hookId, accessoryId, customId), id);
            }
        }
    }

    private Long inferAttributeGroup(String name, Long styleId, Long hookId, Long accessoryId, Long customId) {
        String text = name == null ? "" : name.toLowerCase();
        if (text.contains("钩") || text.contains("hook")) {
            return hookId;
        }
        if (text.contains("配件") || text.contains("下夹") || text.contains("夹")) {
            return accessoryId;
        }
        if (text.contains("定制") || text.contains("刻") || text.contains("logo") || text.contains("填")
                || text.contains("色") || text.contains("红") || text.contains("黑") || text.contains("白")
                || text.contains("特殊") || text.contains("不刻")) {
            return customId;
        }
        return styleId;
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
            if (!checkColumnExists("erp_product", "option_attribute_ids")) {
                log.info("表 erp_product 缺少 option_attribute_ids 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_product ADD column option_attribute_ids varchar(2000) DEFAULT NULL COMMENT '商品可选项ID集合' AFTER attribute_ids");
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
            if (!checkColumnExists("erp_bill_item", "logo_image_url")) {
                log.info("表 erp_bill_item 缺少 logo_image_url 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column logo_image_url varchar(500) DEFAULT NULL COMMENT 'LOGO图片' AFTER product_image_url");
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
            if (!checkColumnExists("erp_bill_item", "option_attribute_quantity_json")) {
                log.info("表 erp_bill_item 缺少 option_attribute_quantity_json 列，创建中...");
                jdbcTemplate.execute("ALTER TABLE erp_bill_item ADD column option_attribute_quantity_json varchar(2000) DEFAULT NULL COMMENT '选配项独立数量JSON' AFTER option_attribute_text");
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

    private Long availableId(long preferredId, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Integer.class,
                preferredId);
        return count != null && count == 0 ? preferredId : jdbcTemplate.queryForObject("SELECT UUID_SHORT()", Long.class);
    }
}
