import request from '@/utils/request'
import type { DeptVO } from './dept'
import type { PageResult } from '@/api/types'

export interface StaffVO {
  userId: string
  deptId?: string
  username: string
  nickname: string
  email?: string
  phone?: string
  gender?: number
  avatar?: string
  status: number
  createTime: string
  remark?: string
  dept?: DeptVO
  roleIds?: string[]
  postIds?: string[]
}

export interface StaffQuery {
  current: number
  size: number
  username?: string
  phone?: string
  status?: number
  deptId?: string
}

export interface StaffForm {
  userId?: string
  deptId?: string
  username: string
  password?: string
  nickname: string
  email?: string
  phone?: string
  gender?: number
  status: number
  remark?: string
  roleIds?: string[]
  postIds?: string[]
}

export function listStaff(params: StaffQuery) {
  return request<PageResult<StaffVO>>({
    url: '/system/staff/list',
    method: 'get',
    params
  })
}

export function getStaff(userId: string) {
  return request<StaffVO>({
    url: `/system/staff/${userId}`,
    method: 'get'
  })
}

export function addStaff(data: StaffForm) {
  return request({
    url: '/system/staff',
    method: 'post',
    data
  })
}

export function updateStaff(data: StaffForm) {
  return request({
    url: '/system/staff',
    method: 'put',
    data
  })
}

export function delStaff(userId: string) {
  return request({
    url: `/system/staff/${userId}`,
    method: 'delete'
  })
}

export function updateStaffStatus(userId: string, status: number) {
  return request({
    url: `/system/staff/${userId}/status`,
    method: 'put',
    params: { status }
  })
}
