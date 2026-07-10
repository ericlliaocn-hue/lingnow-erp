package cc.lingnow.admin.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.lingnow.admin.model.bo.SysStaffQueryBO;
import cc.lingnow.admin.model.bo.SysStaffSaveBO;
import cc.lingnow.admin.model.vo.SysStaffVO;
import cc.lingnow.biz.dept.service.SysDeptService;
import cc.lingnow.biz.post.service.SysUserPostService;
import cc.lingnow.biz.role.service.SysRoleService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.service.SysUserService;
import cc.lingnow.common.constant.CommonConstants;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工管理业务逻辑
 *
 * @author LingNow Team
 */
@Service
@RequiredArgsConstructor
public class SysStaffManager {

    private final SysUserService userService;
    private final SysDeptService deptService;
    private final SysRoleService roleService;
    private final SysUserPostService userPostService;

    /**
     * 查询员工列表
     */
    public PageResult<SysStaffVO> listStaff(SysStaffQueryBO query) {
        Page<SysUser> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<SysUser> lqw = userService.businessVisibleQuery();
        lqw.eq(SysUser::getDelFlag, false)
                .eq(ObjectUtil.isNotNull(query.getDeptId()), SysUser::getDeptId, query.getDeptId())
                .like(ObjectUtil.isNotEmpty(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(ObjectUtil.isNotEmpty(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(ObjectUtil.isNotNull(query.getStatus()), SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = userService.page(page, lqw);

        List<SysStaffVO> voList = result.getRecords().stream()
                .map(item -> {
                    SysStaffVO vo = BeanUtil.copyProperties(item, SysStaffVO.class);
                    if (item.getDeptId() != null) {
                        vo.setDept(deptService.getById(item.getDeptId()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResult.of(query.getCurrent(), query.getSize(), result.getTotal(), voList);
    }

    /**
     * 查询员工详情
     */
    public SysStaffVO getStaff(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        if (userService.isInternalAccount(user)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        SysStaffVO vo = BeanUtil.copyProperties(user, SysStaffVO.class);
        if (user.getDeptId() != null) {
            vo.setDept(deptService.getById(user.getDeptId()));
        }

        // Populate roles and posts
        vo.setRoleIds(roleService.selectRoleIdsByUserId(userId));
        vo.setPostIds(userPostService.selectPostIdsByUserId(userId));

        return vo;
    }

    /**
     * 新增员工
     */
    @Transactional(rollbackFor = Exception.class)
    public void addStaff(SysStaffSaveBO bo) {
        if (userService.getByUsername(bo.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USER_EXIST);
        }
        SysUser user = BeanUtil.copyProperties(bo, SysUser.class);
        if (ObjectUtil.isEmpty(user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新增职员必须设置初始密码");
        } else {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        }
        userService.save(user);

        // Save Post and Role relations
        if (CollUtil.isNotEmpty(bo.getPostIds())) {
            userPostService.assignPosts(user.getUserId(), bo.getPostIds());
        }
        if (CollUtil.isNotEmpty(bo.getRoleIds())) {
            roleService.assignRoles(user.getUserId(), bo.getRoleIds().toArray(new Long[0]));
        }
    }

    /**
     * 修改员工
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStaff(SysStaffSaveBO bo) {
        if (ObjectUtil.isNull(bo.getUserId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        SysUser user = BeanUtil.copyProperties(bo, SysUser.class);
        if (ObjectUtil.isNotEmpty(bo.getPassword())) {
            user.setPassword(BCrypt.hashpw(bo.getPassword()));
        } else {
            user.setPassword(null); // Don't update password if empty
        }
        userService.updateById(user);

        // Update Post and Role relations
        if (bo.getPostIds() != null) {
            userPostService.assignPosts(user.getUserId(), bo.getPostIds());
        }
        if (bo.getRoleIds() != null) {
            roleService.assignRoles(user.getUserId(), bo.getRoleIds().toArray(new Long[0]));
        }
    }

    /**
     * 删除员工
     */
    public void removeStaff(List<Long> userIds) {
        if (CollUtil.contains(userIds, 1L) || userService.listByIds(userIds).stream().anyMatch(userService::isInternalAccount)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR); // Cannot delete admin
        }
        userService.removeByIds(userIds);
    }

    /**
     * 更新员工状态
     */
    public void updateStatus(Long userId, Integer status) {
        SysUser current = userService.getById(userId);
        if (current == null || userService.isInternalAccount(current)) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        if (!CommonConstants.STATUS_NORMAL.equals(status) && !CommonConstants.STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户状态不正确");
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(status);
        userService.updateById(user);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long userId, String password) {
        SysUser current = userService.getById(userId);
        if (userId == 1L || userService.isInternalAccount(current)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Cannot reset admin password");
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPassword(BCrypt.hashpw(password));
        userService.updateById(user);
    }

    /**
     * 分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        SysUser current = userService.getById(userId);
        if (userId == 1L || userService.isInternalAccount(current)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Cannot assign roles to admin");
        }
        roleService.assignRoles(userId, roleIds.toArray(new Long[0]));
    }
}
