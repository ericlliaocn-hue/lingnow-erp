import request from '@/utils/request'

export interface DeptVO {
  deptId: string
  parentId: string
  deptName: string
  orderNum: number
  leader: string
  phone: string
  email: string
  categoryCode: string
  region: string
  status: number
  createTime: string
  children?: DeptVO[]
}

export interface DeptQuery {
  deptName?: string
  status?: number
}

export interface DeptForm {
  deptId?: string
  parentId?: string
  deptName: string
  orderNum: number
  leader?: string
  phone?: string
  email?: string
  categoryCode?: string
  region?: string
  status: number
}

// 查询部门列表
export function listDept(params?: DeptQuery) {
  return request<DeptVO[]>({
    url: '/system/dept/list',
    method: 'get',
    params
  })
}

// 查询部门列表（排除节点）
export function listDeptExcludeChild(deptId: string) {
  return request<DeptVO[]>({
    url: `/system/dept/list/exclude/${deptId}`,
    method: 'get'
  })
}

// 查询部门详细
export function getDept(deptId: string) {
  return request<DeptVO>({
    url: `/system/dept/${deptId}`,
    method: 'get'
  })
}

// 新增部门
export function addDept(data: DeptForm) {
  return request({
    url: '/system/dept',
    method: 'post',
    data
  })
}

// 修改部门
export function updateDept(data: DeptForm) {
  return request({
    url: '/system/dept',
    method: 'put',
    data
  })
}

// 删除部门
export function delDept(deptId: string) {
  return request({
    url: `/system/dept/${deptId}`,
    method: 'delete'
  })
}
