package cc.lingnow.admin.service.impl;

import cc.lingnow.admin.model.bo.erp.ErpApprovalHandleBO;
import cc.lingnow.admin.model.bo.erp.ErpApprovalQueryBO;
import cc.lingnow.admin.model.bo.erp.ErpApprovalSubmitBO;
import cc.lingnow.admin.model.enums.ErpApprovalBizType;
import cc.lingnow.admin.model.enums.ErpApprovalStatus;
import cc.lingnow.admin.model.vo.erp.ErpApprovalHistoryVO;
import cc.lingnow.admin.model.vo.erp.ErpApprovalTaskVO;
import cc.lingnow.admin.service.ErpApprovalService;
import cc.lingnow.admin.service.ErpAuditService;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.erp.entity.ErpBill;
import cc.lingnow.biz.erp.entity.ErpFinanceBill;
import cc.lingnow.biz.erp.entity.ErpStockCheck;
import cc.lingnow.biz.erp.service.ErpBillService;
import cc.lingnow.biz.erp.service.ErpFinanceBillService;
import cc.lingnow.biz.erp.service.ErpStockCheckService;
import cc.lingnow.biz.notification.service.SysUserNotificationService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowFactory;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ErpApprovalServiceImpl implements ErpApprovalService {

    private static final String FLOW_PENDING = "1";
    private static final String FLOW_PASS = "2";
    private static final String FLOW_CANCEL = "6";
    private static final String FLOW_FINISHED = "8";
    private static final String FLOW_REJECT = "9";

    private final ErpBillService billService;
    private final ErpFinanceBillService financeBillService;
    private final ErpStockCheckService stockCheckService;
    private final SysUserService userService;
    private final SysUserNotificationService notificationService;
    private final ErpAuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(ErpApprovalSubmitBO bo) {
        ErpApprovalBizType type = ErpApprovalBizType.of(bo.getBizType());
        StpAdminUtil.stpLogic.checkPermission(type.permissionPrefix() + ":audit");
        ApprovalBill bill = requireBill(type, bo.getBizId());
        ensureCanSubmit(bill);
        List<Long> approvers = approvalUsers();
        if (approvers.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有可审批用户，请先给用户分配审批权限");
        }

        Long submitUserId = StpAdminUtil.getLoginIdAsLong();
        Date now = new Date();
        Definition definition = ensureDefaultDefinition(type);
        Instance instance = new FlowInstance()
                .setId(nextId())
                .setDefinitionId(definition.getId())
                .setFlowName(definition.getFlowName())
                .setBusinessId(businessId(type.name(), bo.getBizId()))
                .setNodeType(1)
                .setNodeCode("APPROVE")
                .setNodeName("审批")
                .setVariable(variable(type, bill, submitUserId))
                .setFlowStatus(FLOW_PENDING)
                .setActivityStatus(1)
                .setCreateBy(String.valueOf(submitUserId))
                .setCreateTime(now)
                .setUpdateTime(now)
                .setExt(ext(type, bill))
                .setDelFlag("0");
        FlowFactory.insService().save(instance);

        Task task = new FlowTask()
                .setId(nextId())
                .setDefinitionId(definition.getId())
                .setInstanceId(instance.getId())
                .setFlowName(definition.getFlowName())
                .setBusinessId(instance.getBusinessId())
                .setNodeCode("APPROVE")
                .setNodeName("审批")
                .setNodeType(1)
                .setFormCustom("N")
                .setCreateTime(now)
                .setUpdateTime(now)
                .setDelFlag("0");
        FlowFactory.taskService().save(task);

        for (Long approver : approvers) {
            User user = FlowFactory.userService().structureUser(task.getId(), String.valueOf(approver), "1", String.valueOf(submitUserId));
            FlowFactory.userService().save(user);
        }

        updateApprovalSubmit(type, bo.getBizId(), instance.getId(), String.valueOf(submitUserId), toLocal(now));
        for (Long approver : approvers) {
            notify(approver, "审批待办：" + type.label(), bill.billNo() + " 等待你审批", bo.getBizId(), type, "/erp/approval/todo");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pass(ErpApprovalHandleBO bo) {
        Task task = requireTask(bo.getTaskId());
        ensureTaskOwner(task);
        BizKey key = parseBusinessId(task.getBusinessId());
        ErpApprovalBizType type = ErpApprovalBizType.of(key.bizType());
        ApprovalBill bill = requireBill(type, key.bizId());
        if (!ErpApprovalStatus.PENDING.equals(bill.approvalStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据不是审批中状态");
        }
        Date now = new Date();
        insertHistory(task, "PASS", FLOW_PASS, bo.getComment(), now);
        task.setDelFlag("1");
        task.setUpdateTime(now);
        FlowFactory.taskService().updateById(task);
        FlowFactory.userService().deleteByTaskIds(List.of(task.getId()));

        Instance instance = FlowFactory.insService().getById(task.getInstanceId());
        instance.setNodeType(2).setNodeCode("END").setNodeName("结束").setFlowStatus(FLOW_FINISHED).setUpdateTime(now);
        FlowFactory.insService().updateById(instance);

        doAudit(type, key.bizId());
        updateApprovalFinish(type, key.bizId(), ErpApprovalStatus.APPROVED, toLocal(now));
        notifySubmitter(bill, "审批通过：" + type.label(), bill.billNo() + " 已审批通过并完成审核", type, "/erp/approval/mine");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(ErpApprovalHandleBO bo) {
        Task task = requireTask(bo.getTaskId());
        ensureTaskOwner(task);
        BizKey key = parseBusinessId(task.getBusinessId());
        ErpApprovalBizType type = ErpApprovalBizType.of(key.bizType());
        ApprovalBill bill = requireBill(type, key.bizId());
        if (!ErpApprovalStatus.PENDING.equals(bill.approvalStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单据不是审批中状态");
        }
        Date now = new Date();
        insertHistory(task, "REJECT", FLOW_REJECT, bo.getComment(), now);
        task.setDelFlag("1");
        task.setUpdateTime(now);
        FlowFactory.taskService().updateById(task);
        FlowFactory.userService().deleteByTaskIds(List.of(task.getId()));

        Instance instance = FlowFactory.insService().getById(task.getInstanceId());
        instance.setFlowStatus(FLOW_REJECT).setUpdateTime(now);
        FlowFactory.insService().updateById(instance);

        updateApprovalFinish(type, key.bizId(), ErpApprovalStatus.REJECTED, toLocal(now));
        notifySubmitter(bill, "审批驳回：" + type.label(), bill.billNo() + " 已被驳回", type, "/erp/approval/mine");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(ErpApprovalSubmitBO bo) {
        ErpApprovalBizType type = ErpApprovalBizType.of(bo.getBizType());
        ApprovalBill bill = requireBill(type, bo.getBizId());
        Long userId = StpAdminUtil.getLoginIdAsLong();
        if (!String.valueOf(userId).equals(bill.approvalSubmitBy())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只能撤回自己发起的审批");
        }
        if (!ErpApprovalStatus.PENDING.equals(bill.approvalStatus()) || bill.approvalInstanceId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前单据不可撤回");
        }
        Date now = new Date();
        List<Task> tasks = FlowFactory.taskService().list(new FlowTask().setInstanceId(bill.approvalInstanceId()).setDelFlag("0"));
        for (Task task : tasks) {
            task.setDelFlag("1").setUpdateTime(now);
            FlowFactory.taskService().updateById(task);
        }
        FlowFactory.taskService().deleteByInsIds(List.of(bill.approvalInstanceId()));
        Instance instance = FlowFactory.insService().getById(bill.approvalInstanceId());
        if (instance != null) {
            instance.setFlowStatus(FLOW_CANCEL).setUpdateTime(now);
            FlowFactory.insService().updateById(instance);
        }
        updateApprovalFinish(type, bo.getBizId(), ErpApprovalStatus.REVOKED, toLocal(now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(ErpApprovalHandleBO bo) {
        Task task = requireTask(bo.getTaskId());
        ensureTaskOwner(task);
        if (bo.getTransferUserId() == null || userService.getById(bo.getTransferUserId()) == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "转交用户不存在");
        }
        FlowFactory.userService().deleteByTaskIds(List.of(task.getId()));
        User user = FlowFactory.userService().structureUser(task.getId(), String.valueOf(bo.getTransferUserId()), "1", String.valueOf(StpAdminUtil.getLoginIdAsLong()));
        FlowFactory.userService().save(user);
        BizKey key = parseBusinessId(task.getBusinessId());
        ErpApprovalBizType type = ErpApprovalBizType.of(key.bizType());
        notify(bo.getTransferUserId(), "审批转交：" + type.label(), "有一条审批任务转交给你", key.bizId(), type, "/erp/approval/todo");
    }

    @Override
    public PageResult<ErpApprovalTaskVO> todo(ErpApprovalQueryBO query) {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        List<User> taskUsers = FlowFactory.userService().listByProcessedBys(null, String.valueOf(userId), "1");
        List<ErpApprovalTaskVO> list = new ArrayList<>();
        for (User user : taskUsers) {
            Task task = FlowFactory.taskService().getById(user.getAssociated());
            if (task != null && "0".equals(task.getDelFlag())) {
                list.add(taskVO(hydrateTaskBusinessId(task)));
            }
        }
        list.sort((a, b) -> nullSafe(b.getCreateTime()).compareTo(nullSafe(a.getCreateTime())));
        return page(filter(list, query), query);
    }

    @Override
    public PageResult<ErpApprovalTaskVO> done(ErpApprovalQueryBO query) {
        String userId = String.valueOf(StpAdminUtil.getLoginIdAsLong());
        List<ErpApprovalTaskVO> list = FlowFactory.hisTaskService().list(new FlowHisTask().setApprover(userId)).stream()
                .map(this::hisTaskVO)
                .filter(Objects::nonNull)
                .sorted((a, b) -> nullSafe(b.getCreateTime()).compareTo(nullSafe(a.getCreateTime())))
                .toList();
        return page(filter(list, query), query);
    }

    @Override
    public PageResult<ErpApprovalTaskVO> mine(ErpApprovalQueryBO query) {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        List<ErpApprovalTaskVO> list = new ArrayList<>();
        billService.list(new QueryWrapper<ErpBill>().eq("approval_submit_by", String.valueOf(userId)).isNotNull("approval_instance_id"))
                .forEach(item -> list.add(taskFromBill(ErpApprovalBizType.of(item.getBillType()), item)));
        financeBillService.list(new QueryWrapper<ErpFinanceBill>().eq("approval_submit_by", String.valueOf(userId)).isNotNull("approval_instance_id"))
                .forEach(item -> list.add(taskFromFinance(ErpApprovalBizType.of(item.getBillType()), item)));
        stockCheckService.list(new QueryWrapper<ErpStockCheck>().eq("approval_submit_by", String.valueOf(userId)).isNotNull("approval_instance_id"))
                .forEach(item -> list.add(taskFromStock(item)));
        list.sort((a, b) -> nullSafe(b.getSubmitTime()).compareTo(nullSafe(a.getSubmitTime())));
        return page(filter(list, query), query);
    }

    @Override
    public List<ErpApprovalHistoryVO> history(String bizType, Long bizId) {
        Instance sample = new FlowInstance().setBusinessId(businessId(ErpApprovalBizType.of(bizType).name(), bizId));
        Instance instance = FlowFactory.insService().getOne(sample);
        if (instance == null) {
            return List.of();
        }
        return FlowFactory.hisTaskService().list(new FlowHisTask().setInstanceId(instance.getId())).stream().map(this::historyVO).toList();
    }

    private Definition ensureDefaultDefinition(ErpApprovalBizType type) {
        String flowCode = "ERP_" + type.name();
        List<Definition> definitions = FlowFactory.defService().list(new FlowDefinition().setFlowCode(flowCode).setIsPublish(1).setDelFlag("0"));
        if (!definitions.isEmpty()) {
            return definitions.get(0);
        }
        Date now = new Date();
        Definition definition = new FlowDefinition()
                .setId(nextId())
                .setFlowCode(flowCode)
                .setFlowName(type.label() + "审批")
                .setCategory("ERP")
                .setVersion("1.0")
                .setIsPublish(1)
                .setFormCustom("N")
                .setActivityStatus(1)
                .setCreateTime(now)
                .setUpdateTime(now)
                .setDelFlag("0");
        FlowFactory.defService().save(definition);
        return definition;
    }

    private List<Long> approvalUsers() {
        return userService.list(new QueryWrapper<SysUser>().eq("status", 1)).stream()
                .filter(user -> "admin".equals(user.getUsername()) || StpAdminUtil.stpLogic.hasPermission(user.getUserId(), "erp:approval:approve"))
                .map(SysUser::getUserId)
                .distinct()
                .toList();
    }

    private void ensureCanSubmit(ApprovalBill bill) {
        if (Integer.valueOf(1).equals(bill.auditStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已审核单据不能提交审批");
        }
        String status = bill.approvalStatus() == null ? ErpApprovalStatus.NONE : bill.approvalStatus();
        if (!(ErpApprovalStatus.NONE.equals(status) || ErpApprovalStatus.REJECTED.equals(status) || ErpApprovalStatus.REVOKED.equals(status))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前审批状态不能提交");
        }
    }

    private void ensureTaskOwner(Task task) {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        List<User> users = FlowFactory.userService().listByAssociatedAndTypes(task.getId(), "1");
        boolean matched = users.stream().anyMatch(user -> String.valueOf(userId).equals(user.getProcessedBy()));
        if (!matched) {
            throw new BusinessException(ErrorCode.NO_AUTH, "当前任务不属于你");
        }
    }

    private Task requireTask(Long taskId) {
        Task task = FlowFactory.taskService().getById(taskId);
        if (task == null || !"0".equals(task.getDelFlag())) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "审批任务不存在");
        }
        return hydrateTaskBusinessId(task);
    }

    private Task hydrateTaskBusinessId(Task task) {
        if (task.getBusinessId() != null && !task.getBusinessId().isBlank()) {
            return task;
        }
        Instance instance = FlowFactory.insService().getById(task.getInstanceId());
        if (instance == null || instance.getBusinessId() == null || instance.getBusinessId().isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "审批任务缺少业务ID");
        }
        task.setBusinessId(instance.getBusinessId());
        return task;
    }

    private ApprovalBill requireBill(ErpApprovalBizType type, Long bizId) {
        if (type == ErpApprovalBizType.STOCK_CHECK) {
            ErpStockCheck check = stockCheckService.getById(bizId);
            if (check == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
            }
            return new ApprovalBill(check.getId(), "STOCK_CHECK", check.getCheckNo(), BigDecimal.ZERO, check.getAuditStatus(),
                    check.getApprovalStatus(), check.getApprovalInstanceId(), check.getApprovalSubmitBy(), check.getApprovalSubmitTime());
        }
        if (type.name().startsWith("SALE") || type.name().startsWith("PURCHASE")) {
            ErpBill bill = billService.getById(bizId);
            if (bill == null || !type.name().equals(bill.getBillType())) {
                throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
            }
            return new ApprovalBill(bill.getId(), bill.getBillType(), bill.getBillNo(), bill.getPayableAmount(), bill.getAuditStatus(),
                    bill.getApprovalStatus(), bill.getApprovalInstanceId(), bill.getApprovalSubmitBy(), bill.getApprovalSubmitTime());
        }
        ErpFinanceBill bill = financeBillService.getById(bizId);
        if (bill == null || !type.name().equals(bill.getBillType())) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return new ApprovalBill(bill.getId(), bill.getBillType(), bill.getBillNo(), bill.getAmount(), bill.getAuditStatus(),
                bill.getApprovalStatus(), bill.getApprovalInstanceId(), bill.getApprovalSubmitBy(), bill.getApprovalSubmitTime());
    }

    private void updateApprovalSubmit(ErpApprovalBizType type, Long bizId, Long instanceId, String userId, LocalDateTime now) {
        if (type == ErpApprovalBizType.STOCK_CHECK) {
            ErpStockCheck check = stockCheckService.getById(bizId);
            check.setApprovalStatus(ErpApprovalStatus.PENDING);
            check.setApprovalInstanceId(instanceId);
            check.setApprovalSubmitBy(userId);
            check.setApprovalSubmitTime(now);
            check.setApprovalFinishTime(null);
            stockCheckService.updateById(check);
        } else if (type.name().startsWith("SALE") || type.name().startsWith("PURCHASE")) {
            ErpBill bill = billService.getById(bizId);
            bill.setApprovalStatus(ErpApprovalStatus.PENDING);
            bill.setApprovalInstanceId(instanceId);
            bill.setApprovalSubmitBy(userId);
            bill.setApprovalSubmitTime(now);
            bill.setApprovalFinishTime(null);
            billService.updateById(bill);
        } else {
            ErpFinanceBill bill = financeBillService.getById(bizId);
            bill.setApprovalStatus(ErpApprovalStatus.PENDING);
            bill.setApprovalInstanceId(instanceId);
            bill.setApprovalSubmitBy(userId);
            bill.setApprovalSubmitTime(now);
            bill.setApprovalFinishTime(null);
            financeBillService.updateById(bill);
        }
    }

    private void updateApprovalFinish(ErpApprovalBizType type, Long bizId, String status, LocalDateTime now) {
        if (type == ErpApprovalBizType.STOCK_CHECK) {
            ErpStockCheck check = stockCheckService.getById(bizId);
            check.setApprovalStatus(status);
            check.setApprovalFinishTime(now);
            stockCheckService.updateById(check);
        } else if (type.name().startsWith("SALE") || type.name().startsWith("PURCHASE")) {
            ErpBill bill = billService.getById(bizId);
            bill.setApprovalStatus(status);
            bill.setApprovalFinishTime(now);
            billService.updateById(bill);
        } else {
            ErpFinanceBill bill = financeBillService.getById(bizId);
            bill.setApprovalStatus(status);
            bill.setApprovalFinishTime(now);
            financeBillService.updateById(bill);
        }
    }

    private void doAudit(ErpApprovalBizType type, Long bizId) {
        if (type == ErpApprovalBizType.STOCK_CHECK) {
            auditService.auditStockCheck(bizId);
        } else if (type.name().startsWith("SALE") || type.name().startsWith("PURCHASE")) {
            auditService.auditBill(bizId);
        } else {
            auditService.auditFinanceBill(bizId);
        }
    }

    private void insertHistory(Task task, String skipType, String flowStatus, String message, Date now) {
        HisTask hisTask = new FlowHisTask()
                .setId(nextId())
                .setDefinitionId(task.getDefinitionId())
                .setFlowName(task.getFlowName())
                .setInstanceId(task.getInstanceId())
                .setTaskId(task.getId())
                .setBusinessId(task.getBusinessId())
                .setNodeCode(task.getNodeCode())
                .setNodeName(task.getNodeName())
                .setNodeType(task.getNodeType())
                .setTargetNodeCode("END")
                .setTargetNodeName("结束")
                .setApprover(String.valueOf(StpAdminUtil.getLoginIdAsLong()))
                .setCooperateType(1)
                .setSkipType(skipType)
                .setFlowStatus(flowStatus)
                .setFormCustom("N")
                .setMessage(message)
                .setCreateTime(task.getCreateTime())
                .setUpdateTime(now)
                .setDelFlag("0");
        FlowFactory.hisTaskService().save(hisTask);
    }

    private ErpApprovalTaskVO taskVO(Task task) {
        BizKey key = parseBusinessId(task.getBusinessId());
        ErpApprovalBizType type = ErpApprovalBizType.of(key.bizType());
        ApprovalBill bill = requireBill(type, key.bizId());
        ErpApprovalTaskVO vo = baseTask(type, bill);
        vo.setTaskId(task.getId());
        vo.setInstanceId(task.getInstanceId());
        vo.setNodeCode(task.getNodeCode());
        vo.setNodeName(task.getNodeName());
        vo.setFlowStatus(FLOW_PENDING);
        vo.setCreateTime(toLocal(task.getCreateTime()));
        return vo;
    }

    private ErpApprovalTaskVO hisTaskVO(HisTask hisTask) {
        if (hisTask == null || hisTask.getBusinessId() == null || hisTask.getBusinessId().isBlank()) {
            return null;
        }
        BizKey key = parseBusinessId(hisTask.getBusinessId());
        ErpApprovalBizType type = ErpApprovalBizType.of(key.bizType());
        ApprovalBill bill = requireBill(type, key.bizId());
        ErpApprovalTaskVO vo = baseTask(type, bill);
        vo.setTaskId(hisTask.getTaskId());
        vo.setInstanceId(hisTask.getInstanceId());
        vo.setNodeCode(hisTask.getNodeCode());
        vo.setNodeName(hisTask.getNodeName());
        vo.setFlowStatus(hisTask.getFlowStatus());
        vo.setCreateTime(toLocal(hisTask.getUpdateTime()));
        return vo;
    }

    private ErpApprovalTaskVO taskFromBill(ErpApprovalBizType type, ErpBill bill) {
        return baseTask(type, new ApprovalBill(bill.getId(), bill.getBillType(), bill.getBillNo(), bill.getPayableAmount(),
                bill.getAuditStatus(), bill.getApprovalStatus(), bill.getApprovalInstanceId(), bill.getApprovalSubmitBy(), bill.getApprovalSubmitTime()));
    }

    private ErpApprovalTaskVO taskFromFinance(ErpApprovalBizType type, ErpFinanceBill bill) {
        return baseTask(type, new ApprovalBill(bill.getId(), bill.getBillType(), bill.getBillNo(), bill.getAmount(),
                bill.getAuditStatus(), bill.getApprovalStatus(), bill.getApprovalInstanceId(), bill.getApprovalSubmitBy(), bill.getApprovalSubmitTime()));
    }

    private ErpApprovalTaskVO taskFromStock(ErpStockCheck check) {
        return baseTask(ErpApprovalBizType.STOCK_CHECK, new ApprovalBill(check.getId(), "STOCK_CHECK", check.getCheckNo(), BigDecimal.ZERO,
                check.getAuditStatus(), check.getApprovalStatus(), check.getApprovalInstanceId(), check.getApprovalSubmitBy(), check.getApprovalSubmitTime()));
    }

    private ErpApprovalTaskVO baseTask(ErpApprovalBizType type, ApprovalBill bill) {
        ErpApprovalTaskVO vo = new ErpApprovalTaskVO();
        vo.setBizType(type.name());
        vo.setBizId(bill.id());
        vo.setBizName(type.label());
        vo.setBillNo(bill.billNo());
        vo.setApprovalStatus(bill.approvalStatus());
        vo.setAmount(bill.amount() == null ? "0" : bill.amount().toPlainString());
        vo.setSubmitBy(bill.approvalSubmitBy());
        vo.setSubmitTime(bill.approvalSubmitTime());
        vo.setActionUrl(actionUrl(type, bill.id()));
        return vo;
    }

    private ErpApprovalHistoryVO historyVO(HisTask hisTask) {
        ErpApprovalHistoryVO vo = new ErpApprovalHistoryVO();
        vo.setId(hisTask.getId());
        vo.setTaskId(hisTask.getTaskId());
        vo.setInstanceId(hisTask.getInstanceId());
        vo.setNodeName(hisTask.getNodeName());
        vo.setTargetNodeName(hisTask.getTargetNodeName());
        vo.setApprover(hisTask.getApprover());
        vo.setSkipType(hisTask.getSkipType());
        vo.setFlowStatus(hisTask.getFlowStatus());
        vo.setMessage(hisTask.getMessage());
        vo.setCreateTime(toLocal(hisTask.getCreateTime()));
        vo.setUpdateTime(toLocal(hisTask.getUpdateTime()));
        return vo;
    }

    private List<ErpApprovalTaskVO> filter(List<ErpApprovalTaskVO> list, ErpApprovalQueryBO query) {
        return list.stream()
                .filter(item -> query.getBizType() == null || query.getBizType().isBlank() || query.getBizType().equals(item.getBizType()))
                .filter(item -> query.getBillNo() == null || query.getBillNo().isBlank() || item.getBillNo().contains(query.getBillNo()))
                .filter(item -> query.getApprovalStatus() == null || query.getApprovalStatus().isBlank() || query.getApprovalStatus().equals(item.getApprovalStatus()))
                .toList();
    }

    private PageResult<ErpApprovalTaskVO> page(List<ErpApprovalTaskVO> list, ErpApprovalQueryBO query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();
        int from = Math.min((int) ((current - 1) * size), list.size());
        int to = Math.min(from + (int) size, list.size());
        return PageResult.of(current, size, (long) list.size(), list.subList(from, to));
    }

    private void notify(Long userId, String title, String content, Long bizId, ErpApprovalBizType type, String actionUrl) {
        notificationService.sendNotification(userId, title, content, "warning", bizId, type.name(), "APPROVAL", "OPEN", actionUrl);
    }

    private void notifySubmitter(ApprovalBill bill, String title, String content, ErpApprovalBizType type, String actionUrl) {
        if (bill.approvalSubmitBy() != null) {
            notify(Long.valueOf(bill.approvalSubmitBy()), title, content, bill.id(), type, actionUrl);
        }
    }

    private String variable(ErpApprovalBizType type, ApprovalBill bill, Long submitUserId) {
        return "{\"bizType\":\"" + type.name() + "\",\"bizId\":\"" + bill.id() + "\",\"billNo\":\"" + bill.billNo() + "\",\"submitUserId\":\"" + submitUserId + "\"}";
    }

    private String ext(ErpApprovalBizType type, ApprovalBill bill) {
        return "bizType=" + type.name() + ";bizId=" + bill.id() + ";billNo=" + bill.billNo();
    }

    private String businessId(String bizType, Long bizId) {
        return bizType + ":" + bizId;
    }

    private BizKey parseBusinessId(String businessId) {
        if (businessId == null || businessId.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "审批业务ID为空");
        }
        String[] parts = businessId.split(":", 2);
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "审批业务ID格式错误");
        }
        return new BizKey(parts[0], Long.valueOf(parts[1]));
    }

    private String actionUrl(ErpApprovalBizType type, Long id) {
        if (type == ErpApprovalBizType.STOCK_CHECK) {
            return "/erp/stock/check-edit?id=" + id;
        }
        if (type.name().startsWith("SALE") || type.name().startsWith("PURCHASE")) {
            return "/erp/" + type.module() + "/edit?id=" + id;
        }
        return "/erp/finance/" + type.module();
    }

    private Long nextId() {
        return System.currentTimeMillis() * 1000 + (long) (Math.random() * 1000);
    }

    private LocalDateTime toLocal(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private LocalDateTime nullSafe(LocalDateTime value) {
        return value == null ? LocalDateTime.MIN : value;
    }

    private record ApprovalBill(Long id, String bizType, String billNo, BigDecimal amount, Integer auditStatus,
                                String approvalStatus, Long approvalInstanceId, String approvalSubmitBy,
                                LocalDateTime approvalSubmitTime) {
    }

    private record BizKey(String bizType, Long bizId) {
    }
}
