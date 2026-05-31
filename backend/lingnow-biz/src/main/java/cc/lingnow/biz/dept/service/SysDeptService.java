package cc.lingnow.biz.dept.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.dept.entity.SysDept;

import java.util.List;

/**
 * 部门Service接口
 *
 * @author LingNow Team
 */
public interface SysDeptService extends IService<SysDept> {

    /**
     * 查询部门列表
     *
     * @param dept 部门信息
     * @return 部门集合
     */
    List<SysDept> selectDeptList(SysDept dept);

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    boolean checkDeptNameUnique(SysDept dept);

    /**
     * 校验是否有子部门
     *
     * @param deptId 部门ID
     * @return 结果
     */
    boolean hasChildByDeptId(Long deptId);

    /**
     * 校验部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果
     */
    boolean checkDeptExistUser(Long deptId);
}
