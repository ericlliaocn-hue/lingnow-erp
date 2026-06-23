package cc.lingnow.admin.manager;

import cc.lingnow.admin.model.bo.AdminLoginBO;
import cc.lingnow.admin.model.bo.AdminUserUpdateBO;
import cc.lingnow.admin.model.bo.UserQueryBO;
import cc.lingnow.admin.model.vo.AdminLoginVO;
import cc.lingnow.admin.model.vo.UserDetailVO;
import cc.lingnow.admin.model.vo.UserListVO;
import cc.lingnow.admin.model.vo.UserStatsVO;
import cc.lingnow.admin.util.StpAdminUtil;
import cc.lingnow.biz.notification.service.SysUserNotificationService;
import cc.lingnow.biz.role.entity.SysRole;
import cc.lingnow.biz.role.service.SysRoleService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端用户业务编排层
 * 负责组合多个 Service 调用，实现管理端特定的业务逻辑
 *
 * @author LingNow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSysUserManager {

    private final SysUserService userService;
    private final SysUserNotificationService notificationService;
    private final SysRoleService roleService;

    /**
     * 管理员登录
     *
     * @param loginBO 登录参数
     * @return 登录结果
     */
    public AdminLoginVO login(AdminLoginBO loginBO) {
        // 1. 查询用户
        SysUser sysUser = userService.getByUsername(loginBO.getUsername());
        if (sysUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        // 2. 验证密码
        if (!BCrypt.checkpw(loginBO.getPassword(), sysUser.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 3. 检查状态
        if (!sysUser.getStatus().equals(CommonConstants.STATUS_NORMAL)) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 4. 执行登录 (Admin专用)
        StpAdminUtil.login(sysUser.getUserId());
        StpAdminUtil.stpLogic.getSession().set("loginUser", sysUser);

        // 5. 构建返回值
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(StpAdminUtil.getTokenValue());
        vo.setUsername(sysUser.getUsername());
        vo.setNickname(sysUser.getNickname());
        vo.setPermissions(StpAdminUtil.stpLogic.getPermissionList());

        log.info("管理员登录成功: username={}, adminId={}", sysUser.getUsername(), sysUser.getUserId());
        if (!userService.isInternalAccount(sysUser)) {
            notificationService.sendNotification(sysUser.getUserId(), "系统消息", "您已成功登录管理端", "info", sysUser.getUserId(), "login");
        }
        return vo;
    }

    /**
     * 分页查询用户列表
     * 管理端场景：查看所有用户，支持条件筛选
     *
     * @param query 查询条件
     * @return 用户分页列表
     */
    public PageResult<UserListVO> listUsers(UserQueryBO query) {
        Page<SysUser> page = new Page<>(query.getCurrent(), query.getSize());

        // 构建查询条件
        LambdaQueryWrapper<SysUser> wrapper = buildQueryWrapper(query);

        IPage<SysUser> result = userService.page(page, wrapper);

        // Entity → VO 转换
        List<UserListVO> voList = result.getRecords().stream()
                .map(user -> BeanUtil.toBean(user, UserListVO.class))
                .collect(Collectors.toList());

        log.debug("管理端查询用户列表: current={}, size={}, total={}",
                query.getCurrent(), query.getSize(), result.getTotal());

        return PageResult.of(result.getCurrent(), result.getSize(),
                result.getTotal(), voList);
    }

    /**
     * 获取用户详情
     * 管理端场景：查看用户完整信息
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    public UserDetailVO getUserDetail(Long userId) {
        SysUser sysUser = userService.getById(userId);
        if (sysUser == null || userService.isInternalAccount(sysUser)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }

        // Entity → VO 转换
        UserDetailVO vo = BeanUtil.toBean(sysUser, UserDetailVO.class);

        // Populate roles
        List<Long> roleIds = roleService.selectRoleIdsByUserId(userId);
        if (CollUtil.isNotEmpty(roleIds)) {
            List<SysRole> roles = roleService.listByIds(roleIds);
            vo.setRoles(roles);
        }

        log.debug("管理端查询用户详情: userId={}", userId);
        return vo;
    }

    /**
     * 管理端更新用户信息。
     *
     * @param userId   用户ID
     * @param updateBO 更新内容
     * @return 是否成功
     */
    public boolean updateUser(Long userId, AdminUserUpdateBO updateBO) {
        ensureBusinessVisibleUser(userId);
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);

        // 只拷贝非 null 字段，避免把未填写字段覆盖为 null
        BeanUtil.copyProperties(updateBO, sysUser, "id", "password", "createTime", "updateTime", "createBy", "updateBy");

        boolean success = userService.updateById(sysUser);
        log.info("管理端更新用户信息: userId={}, success={}", userId, success);
        return success;
    }

    /**
     * 获取用户统计信息，供仪表盘使用
     *
     * @return 用户统计 VO
     */
    public UserStatsVO getUserStats() {
        // 总用户数
        long totalUsers = userService.count(userService.businessVisibleQuery());

        // 禁用用户数
        long disabledUsers = userService.count(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getInternalAccount, 0)
                        .eq(SysUser::getStatus, CommonConstants.STATUS_DISABLED)
        );

        // 今日新增用户数（按 createTime 大于等于今天 00:00 统计）
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNewUsers = userService.count(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getInternalAccount, 0)
                        .ge(SysUser::getCreateTime, todayStart)
        );

        UserStatsVO vo = new UserStatsVO();
        vo.setTotalUsers(totalUsers);
        vo.setDisabledUsers(disabledUsers);
        vo.setTodayNewUsers(todayNewUsers);

        log.debug("管理端用户统计: total={}, disabled={}, todayNew={}",
                totalUsers, disabledUsers, todayNewUsers);

        return vo;
    }

    /**
     * 禁用用户
     * 管理端场景：封禁违规用户
     *
     * @param userId 用户ID
     * @param reason 禁用原因
     * @return 是否成功
     */
    public boolean disableUser(Long userId, String reason) {
        ensureBusinessVisibleUser(userId);
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setStatus(CommonConstants.STATUS_DISABLED);

        boolean success = userService.updateById(sysUser);

        if (success) {
            log.info("管理端禁用用户: userId={}, reason={}", userId, reason);
            // TODO: 这里可以记录操作日志
            // operationLogService.log("禁用用户", userId, reason);
        }

        return success;
    }

    /**
     * 启用用户
     * 管理端场景：解除用户封禁
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    public boolean enableUser(Long userId) {
        ensureBusinessVisibleUser(userId);
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setStatus(CommonConstants.STATUS_NORMAL);

        boolean success = userService.updateById(sysUser);

        if (success) {
            log.info("管理端启用用户: userId={}", userId);
            // TODO: 记录操作日志
        }

        return success;
    }

    /**
     * 更新用户状态
     * 管理端场景：批量或单个更新用户状态
     *
     * @param userId 用户ID
     * @param status 新状态
     * @return 是否成功
     */
    public boolean updateUserStatus(Long userId, Integer status) {
        ensureBusinessVisibleUser(userId);
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setStatus(status);

        boolean success = userService.updateById(sysUser);

        log.info("管理端更新用户状态: userId={}, status={}, success={}", userId, status, success);
        return success;
    }

    private void ensureBusinessVisibleUser(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null || userService.isInternalAccount(user)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
    }

    /**
     * 构建查询条件
     *
     * @param query 查询BO
     * @return 查询条件
     */
    private LambdaQueryWrapper<SysUser> buildQueryWrapper(UserQueryBO query) {
        LambdaQueryWrapper<SysUser> lqw = userService.businessVisibleQuery();
        lqw.like(ObjUtil.isNotEmpty(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(ObjUtil.isNotEmpty(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getCreateTime);
        return lqw;
    }

}
