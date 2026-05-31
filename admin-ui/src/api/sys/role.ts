import request from '@/utils/request'

export interface RoleQuery {
    current: number
    size: number
    roleName?: string
}

export interface Role {
    roleId: string
    roleName: string
    roleKey: string
    sortOrder: number
    status: number
    remark?: string
    createTime: string
    dataScope?: number
    menuIds?: number[]
    deptIds?: number[]
}

export const getRoleList = (params: RoleQuery) => {
    return request({
        url: '/role/list',
        method: 'get',
        params
    })
}

export const getActiveRoles = () => {
    return request({
        url: '/role/active',
        method: 'get'
    })
}

export const getRoleDetail = (roleId: string) => {
    return request({
        url: `/role/${roleId}`,
        method: 'get'
    })
}

export const addRole = (data: Partial<Role>) => {
    return request({
        url: '/role',
        method: 'post',
        data
    })
}

export const updateRole = (data: Role) => {
    return request({
        url: '/role',
        method: 'put',
        data
    })
}

export const deleteRole = (roleId: string) => {
    return request({
        url: `/role/${roleId}`,
        method: 'delete'
    })
}

export const assignRoles = (userId: string, roleIds: string[]) => {
    return request({
        url: '/role/assign',
        method: 'post',
        params: { userId },
        data: roleIds
    })
}

/**
 * 角色数据权限
 */
export function dataScope(data: any) {
  return request({
    url: '/role/dataScope',
    method: 'put',
    data: data
  })
}

/**
 * 查询角色已授权用户列表
 */
export function allocatedUserList(query: any) {
  return request({
    url: '/role/authUser/allocatedList',
    method: 'get',
    params: query
  })
}

/**
 * 查询角色未授权用户列表
 */
export function unallocatedUserList(query: any) {
  return request({
    url: '/role/authUser/unallocatedList',
    method: 'get',
    params: query
  })
}

/**
 * 取消用户授权
 */
export function authUserCancel(data: any) {
  return request({
    url: '/role/authUser/cancel',
    method: 'put',
    data: data
  })
}

/**
 * 批量取消用户授权
 */
export function authUserCancelAll(data: any) {
  return request({
    url: '/role/authUser/cancelAll',
    method: 'put',
    params: data
  })
}

/**
 * 授权用户选择
 */
export function authUserSelectAll(data: any) {
  return request({
    url: '/role/authUser/selectAll',
    method: 'put',
    params: data
  })
}
