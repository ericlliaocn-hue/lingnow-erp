import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export function stockBalance(params: any) {
  return request<PageResult<any>>({ url: '/erp/report/stock-balance', method: 'get', params })
}

export function stockFlow(params: any) {
  return request<PageResult<any>>({ url: '/erp/report/stock-flow', method: 'get', params })
}

export function billDetail(params: any) {
  return request<PageResult<any>>({ url: '/erp/report/bill-detail', method: 'get', params })
}

export function partnerBalance() {
  return request<any[]>({ url: '/erp/report/partner-balance', method: 'get' })
}

export function accountBalance() {
  return request<any[]>({ url: '/erp/report/account-balance', method: 'get' })
}

export function erpSummary() {
  return request<Record<string, any>>({ url: '/erp/report/summary', method: 'get' })
}
