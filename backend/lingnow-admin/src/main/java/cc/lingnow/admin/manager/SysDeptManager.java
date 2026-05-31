package cc.lingnow.admin.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cc.lingnow.admin.model.bo.DeptQueryBO;
import cc.lingnow.admin.model.bo.DeptSaveBO;
import cc.lingnow.admin.model.vo.DeptVO;
import cc.lingnow.biz.dept.entity.SysDept;
import cc.lingnow.biz.dept.service.SysDeptService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理业务逻辑
 *
 * @author LingNow Team
 */
@Service
@RequiredArgsConstructor
public class SysDeptManager {

    private final SysDeptService deptService;

    /**
     * 查询部门列表
     */
    public List<DeptVO> listDepts(DeptQueryBO query) {
        SysDept dept = BeanUtil.copyProperties(query, SysDept.class);
        List<SysDept> list = deptService.selectDeptList(dept);
        return list.stream()
                .map(item -> BeanUtil.copyProperties(item, DeptVO.class))
                .collect(Collectors.toList());
    }

    /**
     * 查询部门详情
     */
    public DeptVO getDept(Long deptId) {
        SysDept dept = deptService.getById(deptId);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return BeanUtil.copyProperties(dept, DeptVO.class);
    }

    /**
     * 新增部门
     */
    public void addDept(DeptSaveBO bo) {
        SysDept dept = BeanUtil.copyProperties(bo, SysDept.class);
        if (!deptService.checkDeptNameUnique(dept)) {
            throw new BusinessException(ErrorCode.DEPT_NAME_EXIST);
        }
        deptService.save(dept);
    }

    /**
     * 修改部门
     */
    public void updateDept(DeptSaveBO bo) {
        if (ObjectUtil.isNull(bo.getDeptId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        SysDept dept = BeanUtil.copyProperties(bo, SysDept.class);
        if (!deptService.checkDeptNameUnique(dept)) {
            throw new BusinessException(ErrorCode.DEPT_NAME_EXIST);
        }
        if (dept.getParentId().equals(dept.getDeptId())) {
            throw new BusinessException(ErrorCode.DEPT_PARENT_ID_ERROR);
        }
        deptService.updateById(dept);
    }

    /**
     * 删除部门
     */
    public void removeDept(Long deptId) {
        if (deptService.hasChildByDeptId(deptId)) {
            throw new BusinessException(ErrorCode.DEPT_HAS_CHILD);
        }
        if (deptService.checkDeptExistUser(deptId)) {
            throw new BusinessException(ErrorCode.DEPT_HAS_USER);
        }
        deptService.removeById(deptId);
    }
}
