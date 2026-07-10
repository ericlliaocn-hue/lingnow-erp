import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export interface CustomerAccount {
  id?: string
  customerId: string
  customerName?: string
  username: string
  password?: string
  nickname?: string
  phone?: string
  status: number
  lastLoginTime?: string
  remark?: string
  createTime?: string
}

export interface CustomerAccountQuery {
  current: number
  size: number
  username?: string
  customerName?: string
  status?: number
}

export function listCustomerAccount(params: CustomerAccountQuery) {
  return request<PageResult<CustomerAccount>>({ url: '/erp/customer-account/list', method: 'get', params })
}

export function addCustomerAccount(data: CustomerAccount) {
  return request({ url: '/erp/customer-account', method: 'post', data })
}

export function updateCustomerAccount(data: CustomerAccount) {
  return request({ url: '/erp/customer-account', method: 'put', data })
}

export function updateCustomerAccountStatus(id: string, status: number) {
  return request({ url: `/erp/customer-account/${id}/status/${status}`, method: 'put' })
}
