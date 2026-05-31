import request from '@/utils/request'

export interface UserQuery {
    current: number
    size: number
    username?: string
    phone?: string
    status?: number
}

export interface User {
    userId: string
    username: string
    nickname: string
    phone: string
    avatar: string
    gender: number
    status: number
    createTime: string
    updateTime?: string
}

export interface UserDetail extends User {
    email?: string
    birthday?: string
    region?: string
    createBy?: string
    updateBy?: string
    roles?: { roleId: string, roleName: string }[]
}

export interface UserUpdatePayload {
    nickname?: string
    phone?: string
    email?: string
    avatar?: string
    gender?: number
    birthday?: string
    region?: string
    status?: number
    roleIds?: string[]
}

export interface UserStats {
    totalUsers: number
    disabledUsers: number
    todayNewUsers: number
}

export const login = (data: any) => {
    return request({
        url: '/auth/login',
        method: 'post',
        data
    })
}

export const logout = () => {
    return request({
        url: '/auth/logout',
        method: 'post'
    })
}

export const getUserList = (params: UserQuery) => {
    return request({
        url: '/user/list',
        method: 'get',
        params
    })
}

export const getUserDetail = (userId: string) => {
    return request({
        url: `/user/${userId}`,
        method: 'get'
    })
}

export const updateUser = (userId: string, data: UserUpdatePayload) => {
    return request({
        url: `/user/${userId}`,
        method: 'put',
        data
    })
}

export const updateUserStatus = (userId: string, status: number) => {
    return request({
        url: `/user/${userId}/status`,
        method: 'put',
        params: { status }
    })
}

export const getUserStats = () => {
    return request({
        url: '/user/stats',
        method: 'get'
    })
}

// System Staff Related Operations (Mixed usage for enhanced management)

export const deleteUser = (userIds: string[]) => {
    return request({
        url: `/system/staff/${userIds.join(',')}`,
        method: 'delete'
    })
}

export const resetUserPassword = (data: { userId: string, password: string }) => {
    return request({
        url: '/system/staff/reset-pwd',
        method: 'put',
        data
    })
}

export const assignUserRoles = (data: { userId: string, roleIds: string[] }) => {
    return request({
        url: '/system/staff/auth-role',
        method: 'put',
        data
    })
}
