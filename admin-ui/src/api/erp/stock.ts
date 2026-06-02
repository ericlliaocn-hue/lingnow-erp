import request from '@/utils/request'
import type { PageResult } from '@/api/types'
import type { ApprovalStatus } from './approval'

export interface StockCheckItem {
  id?: string
  productId: string
  productCode?: string
  productName?: string
  spec?: string
  unitId?: string
  unitName?: string
  warehouseId?: string
  bookQty?: number
  checkQty: number
  diffQty?: number
  costPrice?: number
  diffAmount?: number
  remark?: string
}

export interface StockCheck {
  id?: string
  checkNo?: string
  checkDate: string
  warehouseId: string
  warehouseName?: string
  totalProfitQty?: number
  totalLossQty?: number
  totalProfitAmount?: number
  totalLossAmount?: number
  auditStatus?: number
  approvalStatus?: ApprovalStatus
  approvalInstanceId?: string
  approvalSubmitBy?: string
  approvalSubmitTime?: string
  approvalFinishTime?: string
  remark?: string
  items: StockCheckItem[]
}

export interface StockCheckQuery {
  current: number
  size: number
  billNo?: string
  auditStatus?: number
}

export function listStockCheck(params: StockCheckQuery) {
  return request<PageResult<StockCheck>>({ url: '/erp/stock/check/list', method: 'get', params })
}

export function nextStockCheckNo() {
  return request<string>({ url: '/erp/stock/check/nextNo', method: 'get' })
}

export function getStockCheck(id: string) {
  return request<StockCheck>({ url: `/erp/stock/check/${id}`, method: 'get' })
}

export function addStockCheck(data: StockCheck) {
  return request({ url: '/erp/stock/check', method: 'post', data })
}

export function updateStockCheck(data: StockCheck) {
  return request({ url: '/erp/stock/check', method: 'put', data })
}

export function deleteStockCheck(ids: string | string[]) {
  return request({ url: `/erp/stock/check/${Array.isArray(ids) ? ids.join(',') : ids}`, method: 'delete' })
}

export function auditStockCheck(id: string) {
  return request({ url: `/erp/stock/check/audit/${id}`, method: 'put' })
}

export function unauditStockCheck(id: string) {
  return request({ url: `/erp/stock/check/unaudit/${id}`, method: 'put' })
}
