package cc.lingnow.biz.gen.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.gen.bo.GenTableQueryBO;
import cc.lingnow.biz.gen.entity.GenTable;
import cc.lingnow.biz.gen.entity.GenTableColumn;
import cc.lingnow.biz.gen.mapper.GenMapper;
import cc.lingnow.biz.gen.mapper.GenTableColumnMapper;
import cc.lingnow.biz.gen.service.GenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成 服务层实现
 *
 * @author lingnow
 */
@Slf4j
@Service
public class GenServiceImpl extends ServiceImpl<GenMapper, GenTable> implements GenService {

    @Autowired
    private GenTableColumnMapper genTableColumnMapper;

    @Override
    public List<GenTable> selectDbTableList(GenTableQueryBO genTableQueryBO) {
        return baseMapper.selectDbTableList(genTableQueryBO);
    }

    @Override
    public List<GenTable> selectGenTableList(GenTable genTable) {
        return baseMapper.selectList(new QueryWrapper<GenTable>()
                .like(StringUtils.isNotBlank(genTable.getTableName()), "table_name", genTable.getTableName())
                .like(StringUtils.isNotBlank(genTable.getTableComment()), "table_comment", genTable.getTableComment())
                .orderByDesc("create_time"));
    }

    @Override
    public GenTable selectGenTableById(Long id) {
        GenTable genTable = baseMapper.selectById(id);
        List<GenTableColumn> genTableColumns = genTableColumnMapper.selectList(new QueryWrapper<GenTableColumn>().eq("table_id", id));
        genTable.setColumns(genTableColumns);
        return genTable;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGenTable(GenTable genTable) {
        int row = baseMapper.updateById(genTable);
        if (row > 0) {
            for (GenTableColumn cenTableColumn : genTable.getColumns()) {
                genTableColumnMapper.updateById(cenTableColumn);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importGenTable(String[] tableNames) {
        try {
            List<GenTable> tableList = baseMapper.selectDbTableListByNames(tableNames);
            if (tableList == null || tableList.isEmpty()) {
                log.warn("未找到要导入的表: {}", Arrays.toString(tableNames));
                return;
            }
            for (GenTable table : tableList) {
                String tableName = table.getTableName();
                log.info("开始导入表: {}", tableName);

                // 获取列信息
                List<GenTableColumn> columns = baseMapper.selectDbTableColumnsByName(tableName);
                if (columns == null || columns.isEmpty()) {
                    log.warn("表 {} 没有列信息", tableName);
                }

                // 初始化表信息
                initTable(table, columns);

                // 保存表信息
                int row = baseMapper.insert(table);
                if (row > 0) {
                    // 保存列信息
                    for (GenTableColumn column : columns) {
                        column.setTableId(table.getTableId());
                        genTableColumnMapper.insert(column);
                    }
                    log.info("表 {} 导入成功", tableName);
                } else {
                    log.error("表 {} 保存失败", tableName);
                }
            }
        } catch (Exception e) {
            log.error("导入表结构失败: {}", e.getMessage(), e);
            throw new RuntimeException("导入表结构失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, String> previewCode(String tableName) {
        Map<String, String> dataMap = new LinkedHashMap<>();
        // 1. 获取表信息
        GenTable table = baseMapper.selectOne(new QueryWrapper<GenTable>().eq("table_name", tableName));
        if (table == null) {
            throw new RuntimeException("表结构不存在");
        }
        // 2. 获取列信息
        List<GenTableColumn> columns = genTableColumnMapper.selectList(new QueryWrapper<GenTableColumn>().eq("table_id", table.getTableId()));
        table.setColumns(columns);

        // 3. 初始化表信息 (使用保存的配置，不再重新初始化)
        // initTable(table, columns);

        // 4. 设置Velocity上下文
        VelocityContext context = prepareContext(table);

        // 5. 获取模板列表
        List<String> templates = getTemplateList();

        // 6. 渲染模板
        initVelocity();
        for (String template : templates) {
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);
            dataMap.put(template, sw.toString());
        }
        return dataMap;
    }

    @Override
    public byte[] downloadCode(String tableName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        generatorCode(tableName, zip);
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    @Override
    public byte[] downloadCode(String[] tableNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        for (String tableName : tableNames) {
            generatorCode(tableName, zip);
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    @Override
    public void validateEdit(GenTable genTable) {
        if (genTable.getTableName().equals("gen_table")) {
            throw new RuntimeException("不能修改生成代码的表");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void synchDb(String tableName) {
        GenTable table = baseMapper.selectOne(new QueryWrapper<GenTable>().eq("table_name", tableName));
        if (table == null) {
            throw new RuntimeException("同步数据失败，原表结构不存在");
        }

        List<GenTableColumn> tableColumns = genTableColumnMapper.selectList(new QueryWrapper<GenTableColumn>().eq("table_id", table.getTableId()));
        Map<String, GenTableColumn> tableColumnMap = tableColumns.stream().collect(Collectors.toMap(GenTableColumn::getColumnName, c -> c));

        List<GenTableColumn> dbTableColumns = baseMapper.selectDbTableColumnsByName(tableName);
        if (dbTableColumns == null || dbTableColumns.isEmpty()) {
            throw new RuntimeException("同步数据失败，数据库结构不存在");
        }

        List<GenTableColumn> dbTableColumnList = dbTableColumns.stream().filter(c -> !tableColumnMap.containsKey(c.getColumnName())).collect(Collectors.toList());
        for (GenTableColumn column : dbTableColumnList) {
            initColumn(column);
            column.setTableId(table.getTableId());
            genTableColumnMapper.insert(column);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGenTableByIds(Long[] tableIds) {
        baseMapper.deleteBatchIds(Arrays.asList(tableIds));
        genTableColumnMapper.delete(new QueryWrapper<GenTableColumn>().in("table_id", Arrays.asList(tableIds)));
    }

    private void generatorCode(String tableName, ZipOutputStream zip) {
        // 1. 获取表信息
        GenTable table = baseMapper.selectOne(new QueryWrapper<GenTable>().eq("table_name", tableName));
        if (table == null) {
            return;
        }
        // 2. 获取列信息
        List<GenTableColumn> columns = genTableColumnMapper.selectList(new QueryWrapper<GenTableColumn>().eq("table_id", table.getTableId()));
        table.setColumns(columns);

        // 4. 设置Velocity上下文
        VelocityContext context = prepareContext(table);

        // 5. 获取模板列表
        List<String> templates = getTemplateList();

        // 6. 渲染模板
        initVelocity();
        for (String template : templates) {
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);
            try {
                // 添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, table)));
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.flush();
                zip.closeEntry();
            } catch (Exception e) {
                log.error("渲染模板失败，表名：" + table.getTableName(), e);
            }
        }
    }

    private String getFileName(String template, GenTable table) {
        // 文件名转换逻辑
        String fileName = "";
        String packageName = table.getPackageName();
        String moduleName = table.getModuleName();
        String className = table.getClassName();

        String javaPath = "main/java/" + StringUtils.replace(packageName, ".", "/");
        String mybatisPath = "main/resources/mapper/" + moduleName;
        String vuePath = "vue";

        if (template.contains("domain.java.vm")) {
            fileName = javaPath + "/domain/" + className + ".java";
        } else if (template.contains("mapper.java.vm")) {
            fileName = javaPath + "/mapper/" + className + "Mapper.java";
        } else if (template.contains("service.java.vm")) {
            fileName = javaPath + "/service/" + className + "Service.java"; // Fixed IService naming convention to just Service or IService depending on template
        } else if (template.contains("serviceImpl.java.vm")) {
            fileName = javaPath + "/service/impl/" + className + "ServiceImpl.java";
        } else if (template.contains("controller.java.vm")) {
            fileName = javaPath + "/controller/" + className + "Controller.java";
        } else if (template.contains("manager.java.vm")) {
            fileName = javaPath + "/manager/" + className + "Manager.java";
        } else if (template.contains("mapper.xml.vm")) {
            fileName = mybatisPath + "/" + className + "Mapper.xml";
        } else if (template.contains("index.vue.vm")) {
            fileName = vuePath + "/views/" + moduleName + "/" + table.getBusinessName() + "/index.vue";
        } else if (template.contains("api.ts.vm")) {
            fileName = vuePath + "/api/" + moduleName + "/" + table.getBusinessName() + ".ts";
        }
        return fileName;
    }

    private void initTable(GenTable table, List<GenTableColumn> columns) {
        table.setColumns(columns);
        table.setPackageName("cc.lingnow.biz");
        table.setAuthor("lingnow");

        String originalTableName = table.getTableName();
        String moduleName = originalTableName;
        if (moduleName.startsWith("sys_")) {
            moduleName = moduleName.substring(4);
        } else if (moduleName.startsWith("lingnow_")) {
            moduleName = moduleName.substring(8);
        }
        table.setModuleName(moduleName);
        table.setBusinessName(moduleName);

        table.setClassName(StrUtil.upperFirst(StrUtil.toCamelCase(originalTableName)));
        table.setFunctionName(table.getTableComment());

        // 初始化列属性
        for (GenTableColumn column : columns) {
            initColumn(column);
            if ("1".equals(column.getIsPk())) {
                table.setPkColumn(column);
            }
        }
        if (table.getPkColumn() == null && !columns.isEmpty()) {
            table.setPkColumn(columns.get(0));
        }
    }

    /**
     * 初始化列属性
     */
    private void initColumn(GenTableColumn column) {
        String dataType = getDbType(column.getColumnType());
        String columnName = column.getColumnName();
        column.setJavaField(StrUtil.toCamelCase(columnName));
        column.setJavaType(getJavaType(dataType));

        if (arraysContains(new String[]{"create_time", "update_time", "create_by", "update_by"}, columnName)) {
            column.setIsList("0");
            column.setIsEdit("0");
        } else {
            column.setIsList("1");
            column.setIsEdit("1");
        }
    }

    /**
     * 获取模块名
     */
    private String getModuleName(String packageName) {
        int lastIndex = packageName.lastIndexOf(".");
        int nameLength = packageName.length();
        return StringUtils.substring(packageName, lastIndex + 1, nameLength);
    }

    /**
     * 获取数据库类型字段
     */
    private String getDbType(String columnType) {
        if (StringUtils.indexOf(columnType, "(") > 0) {
            return StringUtils.substringBefore(columnType, "(");
        } else {
            return columnType;
        }
    }

    /**
     * 获取Java类型
     */
    private String getJavaType(String columnType) {
        if (StringUtils.containsAnyIgnoreCase(columnType, "bigint")) {
            return "Long";
        }
        if (StringUtils.containsAnyIgnoreCase(columnType, "tinyint", "smallint", "mediumint", "int", "integer")) {
            return "Integer";
        }
        if (StringUtils.containsAnyIgnoreCase(columnType, "float", "double")) {
            return "Double";
        }
        if (StringUtils.containsAnyIgnoreCase(columnType, "decimal")) {
            return "BigDecimal";
        }
        if (StringUtils.containsAnyIgnoreCase(columnType, "char", "varchar", "text")) {
            return "String";
        }
        if (StringUtils.containsAnyIgnoreCase(columnType, "date", "time", "year")) {
            return "LocalDateTime";
        }
        if (StringUtils.containsAnyIgnoreCase(columnType, "bit")) {
            return "Boolean";
        }
        return "String";
    }

    /**
     * 准备Velocity上下文
     */
    private VelocityContext prepareContext(GenTable table) {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("tableName", table.getTableName());
        velocityContext.put("functionName", StringUtils.isNotEmpty(table.getFunctionName()) ? table.getFunctionName() : "【请填写功能名称】");
        velocityContext.put("ClassName", table.getClassName());
        velocityContext.put("className", StringUtils.uncapitalize(table.getClassName()));
        velocityContext.put("moduleName", table.getModuleName());
        velocityContext.put("BusinessName", StrUtil.upperFirst(table.getBusinessName()));
        velocityContext.put("businessName", table.getBusinessName());
        velocityContext.put("basePackage", table.getPackageName());
        velocityContext.put("packageName", table.getPackageName());
        velocityContext.put("author", table.getAuthor());
        velocityContext.put("datetime", DateUtil.now());
        velocityContext.put("pkColumn", table.getPkColumn());
        velocityContext.put("importList", getImportList(table));
        velocityContext.put("columns", table.getColumns());
        velocityContext.put("table", table);
        return velocityContext;
    }

    /**
     * 获取导入包列表
     */
    private List<String> getImportList(GenTable table) {
        List<String> importList = new ArrayList<>();
        for (GenTableColumn column : table.getColumns()) {
            if (column.getJavaType().equals("Date")) {
                importList.add("java.util.Date");
            } else if (column.getJavaType().equals("BigDecimal")) {
                importList.add("java.math.BigDecimal");
            } else if (column.getJavaType().equals("LocalDateTime")) {
                importList.add("java.time.LocalDateTime");
            }
        }
        return importList;
    }

    /**
     * 初始化Velocity
     */
    private void initVelocity() {
        Properties p = new Properties();
        try {
            // 加载classpath目录下的vm文件
            p.setProperty("resource.loader", "classpath");
            p.setProperty("classpath.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            // 定义字符集
            p.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
            // 初始化Velocity引擎，指定配置Properties
            Velocity.init(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取模板列表
     */
    private List<String> getTemplateList() {
        List<String> templates = new ArrayList<>();
        templates.add("vm/java/domain.java.vm");
        templates.add("vm/java/mapper.java.vm");
        templates.add("vm/java/service.java.vm");
        templates.add("vm/java/serviceImpl.java.vm");
        templates.add("vm/java/controller.java.vm");
        templates.add("vm/java/manager.java.vm");
        templates.add("vm/xml/mapper.xml.vm");
        templates.add("vm/vue/index.vue.vm");
        templates.add("vm/vue/api.ts.vm");
        return templates;
    }

    private boolean arraysContains(String[] arr, String targetValue) {
        return Arrays.asList(arr).contains(targetValue);
    }
}
