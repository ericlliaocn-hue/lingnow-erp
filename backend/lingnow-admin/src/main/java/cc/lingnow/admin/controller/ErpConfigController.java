package cc.lingnow.admin.controller;

import cc.lingnow.admin.model.bo.erp.ErpBillNoRuleSaveBO;
import cc.lingnow.admin.model.bo.erp.ErpFieldSettingSaveBO;
import cc.lingnow.admin.model.bo.erp.ErpPrintTemplateSaveBO;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.ErpBillNoRule;
import cc.lingnow.biz.erp.entity.ErpFieldSetting;
import cc.lingnow.biz.erp.entity.ErpPrintTemplate;
import cc.lingnow.biz.erp.service.ErpBillNoRuleService;
import cc.lingnow.biz.erp.service.ErpFieldSettingService;
import cc.lingnow.biz.erp.service.ErpPrintTemplateService;
import cc.lingnow.common.annotation.Log;
import cc.lingnow.common.enums.BusinessType;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cc.lingnow.common.vo.Result;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/erp/config")
@RequiredArgsConstructor
public class ErpConfigController {

    private final ErpBillNoRuleService billNoRuleService;
    private final ErpFieldSettingService fieldSettingService;
    private final ErpPrintTemplateService printTemplateService;

    @GetMapping("/bill-no-rule/list")
    public Result<PageResult<ErpBillNoRule>> billNoRuleList(@RequestParam(defaultValue = "1") Long current,
                                                            @RequestParam(defaultValue = "10") Long size,
                                                            String billType,
                                                            String billName,
                                                            Integer enabled) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:bill-no-rule:list");
        QueryWrapper<ErpBillNoRule> wrapper = new QueryWrapper<ErpBillNoRule>()
                .like(StrUtil.isNotBlank(billType), "bill_type", billType)
                .like(StrUtil.isNotBlank(billName), "bill_name", billName)
                .eq(enabled != null, "enabled", enabled)
                .orderByAsc("bill_type");
        IPage<ErpBillNoRule> page = billNoRuleService.page(new Page<>(current, size), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @GetMapping("/bill-no-rule/{id}")
    public Result<ErpBillNoRule> getBillNoRule(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:bill-no-rule:list");
        ErpBillNoRule rule = billNoRuleService.getById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return Result.success(rule);
    }

    @PostMapping("/bill-no-rule")
    @Log(title = "ERP单号规则", businessType = BusinessType.INSERT)
    public Result<Void> addBillNoRule(@Valid @RequestBody ErpBillNoRuleSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:bill-no-rule:add");
        ensureBillTypeUnique(bo.getBillType(), null);
        billNoRuleService.save(toBillNoRule(bo));
        return Result.success();
    }

    @PutMapping("/bill-no-rule")
    @Log(title = "ERP单号规则", businessType = BusinessType.UPDATE)
    public Result<Void> editBillNoRule(@Valid @RequestBody ErpBillNoRuleSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        StpAdminUtil.stpLogic.checkPermission("erp:config:bill-no-rule:edit");
        ensureBillTypeUnique(bo.getBillType(), bo.getId());
        billNoRuleService.updateById(toBillNoRule(bo));
        return Result.success();
    }

    @DeleteMapping("/bill-no-rule/{ids}")
    @Log(title = "ERP单号规则", businessType = BusinessType.DELETE)
    public Result<Void> removeBillNoRule(@PathVariable List<Long> ids) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:bill-no-rule:remove");
        billNoRuleService.removeByIds(ids);
        return Result.success();
    }

    @GetMapping("/field-setting/list")
    public Result<PageResult<ErpFieldSetting>> fieldSettingList(@RequestParam(defaultValue = "1") Long current,
                                                                @RequestParam(defaultValue = "10") Long size,
                                                                String moduleCode,
                                                                String fieldKey,
                                                                String fieldLabel) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:field-setting:list");
        QueryWrapper<ErpFieldSetting> wrapper = new QueryWrapper<ErpFieldSetting>()
                .like(StrUtil.isNotBlank(moduleCode), "module_code", moduleCode)
                .like(StrUtil.isNotBlank(fieldKey), "field_key", fieldKey)
                .like(StrUtil.isNotBlank(fieldLabel), "field_label", fieldLabel)
                .orderByAsc("module_code")
                .orderByAsc("sort_order");
        IPage<ErpFieldSetting> page = fieldSettingService.page(new Page<>(current, size), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @GetMapping("/field-setting/{id}")
    public Result<ErpFieldSetting> getFieldSetting(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:field-setting:list");
        ErpFieldSetting setting = fieldSettingService.getById(id);
        if (setting == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return Result.success(setting);
    }

    @PostMapping("/field-setting")
    @Log(title = "ERP字段设置", businessType = BusinessType.INSERT)
    public Result<Void> addFieldSetting(@Valid @RequestBody ErpFieldSettingSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:field-setting:add");
        ensureFieldUnique(bo.getModuleCode(), bo.getFieldKey(), null);
        fieldSettingService.save(toFieldSetting(bo));
        return Result.success();
    }

    @PutMapping("/field-setting")
    @Log(title = "ERP字段设置", businessType = BusinessType.UPDATE)
    public Result<Void> editFieldSetting(@Valid @RequestBody ErpFieldSettingSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        StpAdminUtil.stpLogic.checkPermission("erp:config:field-setting:edit");
        ensureFieldUnique(bo.getModuleCode(), bo.getFieldKey(), bo.getId());
        fieldSettingService.updateById(toFieldSetting(bo));
        return Result.success();
    }

    @DeleteMapping("/field-setting/{ids}")
    @Log(title = "ERP字段设置", businessType = BusinessType.DELETE)
    public Result<Void> removeFieldSetting(@PathVariable List<Long> ids) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:field-setting:remove");
        fieldSettingService.removeByIds(ids);
        return Result.success();
    }

    @GetMapping("/print-template/list")
    public Result<PageResult<ErpPrintTemplate>> printTemplateList(@RequestParam(defaultValue = "1") Long current,
                                                                  @RequestParam(defaultValue = "10") Long size,
                                                                  String templateCode,
                                                                  String templateName,
                                                                  String billType,
                                                                  Integer status) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:print-template:list");
        QueryWrapper<ErpPrintTemplate> wrapper = new QueryWrapper<ErpPrintTemplate>()
                .like(StrUtil.isNotBlank(templateCode), "template_code", templateCode)
                .like(StrUtil.isNotBlank(templateName), "template_name", templateName)
                .eq(StrUtil.isNotBlank(billType), "bill_type", billType)
                .eq(status != null, "status", status)
                .orderByDesc("is_default")
                .orderByDesc("create_time");
        IPage<ErpPrintTemplate> page = printTemplateService.page(new Page<>(current, size), wrapper);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @GetMapping("/print-template/{id}")
    public Result<ErpPrintTemplate> getPrintTemplate(@PathVariable Long id) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:print-template:list");
        ErpPrintTemplate template = printTemplateService.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return Result.success(template);
    }

    @PostMapping("/print-template")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP打印模板", businessType = BusinessType.INSERT)
    public Result<Void> addPrintTemplate(@Valid @RequestBody ErpPrintTemplateSaveBO bo) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:print-template:add");
        ensureTemplateCodeUnique(bo.getTemplateCode(), null);
        ErpPrintTemplate template = toPrintTemplate(bo);
        clearDefaultIfNeeded(template);
        printTemplateService.save(template);
        return Result.success();
    }

    @PutMapping("/print-template")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "ERP打印模板", businessType = BusinessType.UPDATE)
    public Result<Void> editPrintTemplate(@Valid @RequestBody ErpPrintTemplateSaveBO bo) {
        if (bo.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        StpAdminUtil.stpLogic.checkPermission("erp:config:print-template:edit");
        ensureTemplateCodeUnique(bo.getTemplateCode(), bo.getId());
        ErpPrintTemplate template = toPrintTemplate(bo);
        clearDefaultIfNeeded(template);
        printTemplateService.updateById(template);
        return Result.success();
    }

    @DeleteMapping("/print-template/{ids}")
    @Log(title = "ERP打印模板", businessType = BusinessType.DELETE)
    public Result<Void> removePrintTemplate(@PathVariable List<Long> ids) {
        StpAdminUtil.stpLogic.checkPermission("erp:config:print-template:remove");
        printTemplateService.removeByIds(ids);
        return Result.success();
    }

    private ErpBillNoRule toBillNoRule(ErpBillNoRuleSaveBO bo) {
        ErpBillNoRule rule = BeanUtil.copyProperties(bo, ErpBillNoRule.class);
        if (rule.getDatePattern() == null) {
            rule.setDatePattern("yyyyMMdd");
        }
        if (rule.getResetCycle() == null) {
            rule.setResetCycle("DAY");
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        return rule;
    }

    private ErpFieldSetting toFieldSetting(ErpFieldSettingSaveBO bo) {
        ErpFieldSetting setting = BeanUtil.copyProperties(bo, ErpFieldSetting.class);
        if (setting.getVisible() == null) {
            setting.setVisible(1);
        }
        if (setting.getRequired() == null) {
            setting.setRequired(0);
        }
        if (setting.getSortOrder() == null) {
            setting.setSortOrder(0);
        }
        return setting;
    }

    private ErpPrintTemplate toPrintTemplate(ErpPrintTemplateSaveBO bo) {
        ErpPrintTemplate template = BeanUtil.copyProperties(bo, ErpPrintTemplate.class);
        if (template.getPaperType() == null) {
            template.setPaperType("A4");
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        return template;
    }

    private void ensureBillTypeUnique(String billType, Long id) {
        ErpBillNoRule exists = billNoRuleService.getOne(new QueryWrapper<ErpBillNoRule>().eq("bill_type", billType).last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据类型规则已存在");
        }
    }

    private void ensureFieldUnique(String moduleCode, String fieldKey, Long id) {
        ErpFieldSetting exists = fieldSettingService.getOne(new QueryWrapper<ErpFieldSetting>()
                .eq("module_code", moduleCode).eq("field_key", fieldKey).last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "字段配置已存在");
        }
    }

    private void ensureTemplateCodeUnique(String templateCode, Long id) {
        ErpPrintTemplate exists = printTemplateService.getOne(new QueryWrapper<ErpPrintTemplate>().eq("template_code", templateCode).last("limit 1"));
        if (exists != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "模板编码已存在");
        }
    }

    private void clearDefaultIfNeeded(ErpPrintTemplate template) {
        if (!Integer.valueOf(1).equals(template.getIsDefault())) {
            return;
        }
        printTemplateService.list(new QueryWrapper<ErpPrintTemplate>()
                        .eq("bill_type", template.getBillType())
                        .eq("is_default", 1))
                .forEach(item -> {
                    if (template.getId() == null || !template.getId().equals(item.getId())) {
                        item.setIsDefault(0);
                        printTemplateService.updateById(item);
                    }
                });
    }
}
