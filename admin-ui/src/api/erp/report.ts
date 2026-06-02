import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export function stockBalance(params: any) {
  return request<PageResult<any>>({ url: '/erp/report/stock-balance', method: 'get', params })
}

export function stockFlow(params: any) {
  return request<PageResult<any>>({ url: '/erp/report/stock-flow', method: 'get', params })
}

export function stockWarning(params: any) {
  return request<PageResult<any>>({ url: '/erp/stock/warning/list', method: 'get', params })
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

export function billStat(params: any) {
  return request<any[]>({ url: '/erp/report/bill-stat', method: 'get', params })
}

export function profitReport(params: any) {
  return request<any[]>({ url: '/erp/report/profit', method: 'get', params })
}

export function trendReport(params: any) {
  return request<any[]>({ url: '/erp/report/trend', method: 'get', params })
}

export function businessProfit(params: any) {
  return request<Record<string, any>>({ url: '/erp/report/business-profit', method: 'get', params })
}

export function hotProducts(params: any) {
  return request<any[]>({ url: '/erp/report/hot-products', method: 'get', params })
}

export function employeePerformance(params: any) {
  return request<any[]>({ url: '/erp/report/employee-performance', method: 'get', params })
}

export function stockSummary(params: any) {
  return request<any[]>({ url: '/erp/report/stock-summary', method: 'get', params })
}

export function inventoryChange(params: any) {
  return request<any[]>({ url: '/erp/report/inventory-change', method: 'get', params })
}

export function exportReport(params: any) {
  return request<Blob>({ url: '/erp/report/export', method: 'get', params, responseType: 'blob' })
}
