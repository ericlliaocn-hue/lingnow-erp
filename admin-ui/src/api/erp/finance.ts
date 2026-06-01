import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export type FinanceModule = 'receipt' | 'payment'

export interface FinanceBill {
  id?: string
  billNo?: string
  billType?: string
  billDate: string
  partnerId: string
  partnerName?: string
  accountId: string
  accountName?: string
  amount: number
  auditStatus?: number
  remark?: string
}

export interface FinanceQuery {
  current: number
  size: number
  billNo?: string
  partnerId?: string
  accountId?: string
  auditStatus?: number
}

export function listFinanceBill(module: FinanceModule, params: FinanceQuery) {
  return request<PageResult<FinanceBill>>({ url: `/erp/finance/${module}/list`, method: 'get', params })
}

export function nextFinanceNo(module: FinanceModule) {
  return request<string>({ url: `/erp/finance/${module}/nextNo`, method: 'get' })
}

export function getFinanceBill(module: FinanceModule, id: string) {
  return request<FinanceBill>({ url: `/erp/finance/${module}/${id}`, method: 'get' })
}

export function addFinanceBill(module: FinanceModule, data: FinanceBill) {
  return request({ url: `/erp/finance/${module}`, method: 'post', data })
}

export function updateFinanceBill(module: FinanceModule, data: FinanceBill) {
  return request({ url: `/erp/finance/${module}`, method: 'put', data })
}

export function deleteFinanceBill(module: FinanceModule, ids: string | string[]) {
  return request({ url: `/erp/finance/${module}/${Array.isArray(ids) ? ids.join(',') : ids}`, method: 'delete' })
}

export function auditFinanceBill(module: FinanceModule, id: string) {
  return request({ url: `/erp/finance/${module}/audit/${id}`, method: 'put' })
}

export function unauditFinanceBill(module: FinanceModule, id: string) {
  return request({ url: `/erp/finance/${module}/unaudit/${id}`, method: 'put' })
}

export function listFundFlow(params: FinanceQuery) {
  return request<PageResult<any>>({ url: '/erp/finance/fund-flow/list', method: 'get', params })
}

export function listPartnerFlow(params: FinanceQuery) {
  return request<PageResult<any>>({ url: '/erp/finance/partner-flow/list', method: 'get', params })
}
