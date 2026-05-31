package cc.lingnow.biz.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.dept.entity.SysDept;
import cc.lingnow.biz.dept.mapper.SysDeptMapper;
import cc.lingnow.biz.dept.service.SysDeptService;
import cc.lingnow.biz.user.entity.SysUser;
import cc.lingnow.biz.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门Service业务层处理
 *
 * @author LingNow Team
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    private final SysUserMapper sysUserMapper;

    @Override
    public List<SysDept> selectDeptList(SysDept dept) {
        LambdaQueryWrapper<SysDept> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SysDept::getDelFlag, false)
                .eq(dept.getDeptId() != null, SysDept::getDeptId, dept.getDeptId())
                .eq(dept.getParentId() != null, SysDept::getParentId, dept.getParentId())
                .like(ObjectUtils.isNotEmpty(dept.getDeptName()), SysDept::getDeptName, dept.getDeptName())
                .eq(dept.getStatus() != null, SysDept::getStatus, dept.getStatus())
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getOrderNum);
        return baseMapper.selectList(lqw);
    }

    @Override
    public boolean checkDeptNameUnique(SysDept dept) {
        Long deptId = ObjectUtils.isEmpty(dept.getDeptId()) ? -1L : dept.getDeptId();
        SysDept info = baseMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptName, dept.getDeptName())
                .eq(SysDept::getParentId, dept.getParentId())
                .last("limit 1"));
        return ObjectUtils.isEmpty(info) || info.getDeptId().longValue() == deptId.longValue();
    }

    @Override
    public boolean hasChildByDeptId(Long deptId) {
        return baseMapper.exists(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, deptId)
                .eq(SysDept::getDelFlag, false));
    }

    @Override
    public boolean checkDeptExistUser(Long deptId) {
        return sysUserMapper.exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getDelFlag, false));
    }
}
