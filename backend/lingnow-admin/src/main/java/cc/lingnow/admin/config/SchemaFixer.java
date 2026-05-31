package cc.lingnow.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库Schema修复工具
 * 用于解决开发环境中数据库表结构不一致的问题
 */
@Slf4j
@Component
public class SchemaFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始检查并修复数据库Schema...");
        fixGenTable();
        fixGenTableColumn();
        log.info("数据库Schema检查修复完成");
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

    private boolean checkColumnExists(String tableName, String columnName) {
        String sql = "SELECT count(*) FROM information_schema.columns WHERE table_schema = (SELECT DATABASE()) AND table_name = ? AND column_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
